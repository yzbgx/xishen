package com.xishen.world;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 灰界的"环境伤害"驱动：每秒给灰界里所有活物挂一次灰蚀。
 *
 * 等级由 GrayRealmRegions 按坐标算：实体区域 0 级 → 灰雾带 1 级（并失明）→ 边缘 2 级起，
 * 离实体区域越远越高（并越来越暗，模拟浓雾）。效果本身每 2 秒结算一次伤害，见 GrayErosionEffect。
 */
public final class GrayRealmTicker {
	private GrayRealmTicker() {
	}

	public static final ResourceKey<Level> GRAY_REALM = ResourceKey.create(Registries.DIMENSION, XishenMod.id("gray_realm"));

	/**
	 * 灰界豁免：实体类型写进这个标签就不吃灰蚀。
	 * 文件：data/xishen/tags/entity_type/gray_realm_immune.json（以后加灰界生物时把它们的 id 填进去）
	 */
	public static final TagKey<EntityType<?>> GRAY_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, XishenMod.id("gray_realm_immune"));

	/**
	 * 实体身上的 scoreboard tag：带上它就豁免灰蚀。
	 * 给"拥有神道领域的玩家/生物"预留的钩子 —— 以后用 /tag <实体> add xishen_gray_immune 就能生效。
	 */
	public static final String IMMUNE_SCOREBOARD_TAG = "xishen_gray_immune";

	/** 每次给多长的持续时间（每秒刷新一次，所以给 60 tick 足够） */
	private static final int REFRESH_DURATION = 60;

	/** 这个实体是否免疫灰界侵蚀 */
	public static boolean isImmune(LivingEntity entity) {
		return entity.getType().is(GRAY_IMMUNE) || entity.getTags().contains(IMMUNE_SCOREBOARD_TAG);
	}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(GrayRealmTicker::onWorldTick);
	}

	private static void onWorldTick(ServerLevel level) {
		if (!level.dimension().equals(GRAY_REALM)) {
			return;
		}
		// 每秒处理一次，避免每 tick 都遍历实体
		if (level.getGameTime() % 20L != 0L) {
			return;
		}

		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}
			// 灰界生物 / 神道领域：免疫灰界侵蚀
			if (isImmune(living)) {
				continue;
			}
			double x = living.getX();
			double z = living.getZ();
			GrayRealmRegions.Zone zone = GrayRealmRegions.zoneAt(x, z);
			int level0 = GrayRealmRegions.erosionLevel(x, z);

			living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(XishenMod.GRAY_EROSION),
					REFRESH_DURATION, level0, true, false, true));

			if (zone == GrayRealmRegions.Zone.FOG) {
				// 灰雾带：失明
				living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, REFRESH_DURATION, 0, true, false, true));
			} else if (zone == GrayRealmRegions.Zone.EDGE) {
				// 边缘：更浓的雾（原版黑暗效果压暗视野）+ 失明
				living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, REFRESH_DURATION, 0, true, false, true));
				living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, REFRESH_DURATION, 0, true, false, true));
			}
		}
	}
}
