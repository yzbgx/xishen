package com.xishen.world;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.Holder;
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
import net.minecraft.world.level.biome.Biome;

/**
 * 灰界的"环境伤害"驱动：每秒给灰界里所有活物挂一次灰蚀。
 *
 * 分三种地带（由 GrayRealmLayout 按坐标算）：
 *   区域（禁忌之海）  普通灰蚀 0 级
 *   灰雾带            普通灰蚀 1 级起 + 失明（豁免依然有效）
 *   边界（超出地图半径）**「灰蚀：边界」——无视任何豁免，强制挂上**，等级随离边界距离递增 + 失明 + 黑暗
 */
public final class GrayRealmTicker {
	private GrayRealmTicker() {
	}

	public static final ResourceKey<Level> GRAY_REALM = ResourceKey.create(Registries.DIMENSION, XishenMod.id("gray_realm"));

	/** 灰界豁免：实体类型写进这个标签就不吃普通灰蚀（边界仍然会强制侵蚀） */
	public static final TagKey<EntityType<?>> GRAY_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, XishenMod.id("gray_realm_immune"));

	/** 实体身上的 scoreboard tag：带上它就豁免普通灰蚀（边界除外） */
	public static final String IMMUNE_SCOREBOARD_TAG = "xishen_gray_immune";

	/** 灰界里的三个群系 */
	public static final ResourceKey<Biome> REGION_BIOME = ResourceKey.create(Registries.BIOME, XishenMod.id("forbidden_sea"));
	public static final ResourceKey<Biome> FOG_BIOME = ResourceKey.create(Registries.BIOME, XishenMod.id("gray_fog"));
	public static final ResourceKey<Biome> EDGE_BIOME = ResourceKey.create(Registries.BIOME, XishenMod.id("gray_fog_edge"));

	private static final int REFRESH_DURATION = 60;

	/** 这个实体是否免疫普通灰蚀 */
	public static boolean isImmune(LivingEntity entity) {
		return entity.getType().is(GRAY_IMMUNE) || entity.getTags().contains(IMMUNE_SCOREBOARD_TAG);
	}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(GrayRealmTicker::onWorldTick);
	}

	/** 取出这个世界正在用的布局（由 biome source 在生成区块时创建） */
	private static GrayRealmLayout layoutOf(ServerLevel level) {
		if (level.getChunkSource().getGenerator().getBiomeSource() instanceof GrayRealmBiomeSource source) {
			return source.layout();
		}
		return null;
	}

	private static void onWorldTick(ServerLevel level) {
		if (!level.dimension().equals(GRAY_REALM)) {
			return;
		}
		if (level.getGameTime() % 20L != 0L) {
			return;
		}
		GrayRealmLayout worldLayout = layoutOf(level);
		if (worldLayout == null) {
			return;
		}

		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}
			double x = living.getX();
			double z = living.getZ();
			Holder<Biome> biome = level.getBiome(living.blockPosition());

			// ① 边界：无视豁免，强制挂「灰蚀：边界」
			double fromCenter = GrayRealmLayout.distanceFromCenter(x, z);
			if (fromCenter > GrayRealmRegions.WORLD_RADIUS) {
				int edgeLevel = (int) Math.min(4, (fromCenter - GrayRealmRegions.WORLD_RADIUS) / GrayRealmRegions.EDGE_LEVEL_STEP);
				living.addEffect(new MobEffectInstance(
						BuiltInRegistries.MOB_EFFECT.wrapAsHolder(XishenMod.GRAY_EROSION_EDGE),
						REFRESH_DURATION, edgeLevel, true, false, true));
				if (biome.is(EDGE_BIOME)) {
					living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, REFRESH_DURATION, 0, true, false, true));
					living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, REFRESH_DURATION, 0, true, false, true));
				}
				continue;
			}

			// ② 灰界生物 / 神道领域：豁免普通灰蚀
			if (isImmune(living)) {
				continue;
			}
			int level0 = worldLayout.erosionLevel(x, z);
			living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(XishenMod.GRAY_EROSION),
					REFRESH_DURATION, level0, true, false, true));
			living.addEffect(new MobEffectInstance(MobEffects.HUNGER, REFRESH_DURATION, 0, true, false, true));

			if (biome.is(FOG_BIOME)) {
				living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, REFRESH_DURATION, 0, true, false, true));
			}
		}
	}
}
