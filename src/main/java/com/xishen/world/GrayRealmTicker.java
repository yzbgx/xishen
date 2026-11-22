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
 * 灰界的"环境伤害"驱动：每秒给灰界里所有活物挂一次效果。
 *
 * 分三种地带（由 GrayRealmLayout 按坐标算）：
 *   区域（禁忌之海 / 鬼嘲深渊）  普通灰蚀 0 级
 *   灰雾带                      普通灰蚀 1 级起 + 失明（豁免依然有效）
 *   边界（超出地图半径）          「灰蚀：边界」——无视任何豁免，强制挂上，等级随距离递增 + 失明 + 黑暗
 *
 * <pre>
 * ============================ 想加"全局效果"看这里 ============================
 * 方法 onWorldTick 里的执行顺序决定了"谁能吃到效果"，一共 4 个可插入位置：
 *
 *   位置 A：拿到 biome 之后、任何分支之前
 *           → 灰界里**所有活物**都会执行到这里（包括免疫的、包括边界的）
 *           → 想让"连免疫生物也中招"的效果（比如饥饿）放这里
 *
 *   位置 B：边界分支内部、continue 之前
 *           → 只有"超出地图半径"的活物会执行
 *           → 想让边界额外附带什么效果放这里
 *
 *   位置 C：isImmune(...) 判断之后
 *           → 从这里开始，被豁免的活物（灰界生物 / 神道领域）已经 continue 走了
 *
 *   位置 D：普通灰蚀旁边
 *           → 只对"区域 + 灰雾带、且没被豁免"的活物生效
 *
 * 例子（想让全灰界都饿，连免疫也饿）：
 *   在位置 A 加一行
 *     living.addEffect(new MobEffectInstance(MobEffects.HUNGER, REFRESH_DURATION, 0, true, false, true));
 * 例子（只想让边界饿）：
 *   在位置 B 加同一行。
 * ==========================================================================
 * </pre>
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

	/** 每次给多长的持续时间（每秒刷新一次，所以给 60 tick 足够，走开 3 秒后自然消失） */
	public static final int REFRESH_DURATION = 60;

	/** 这个实体是否免疫普通灰蚀 */
	public static boolean isImmune(LivingEntity entity) {
		return entity.getType().is(GRAY_IMMUNE) || entity.getTags().contains(IMMUNE_SCOREBOARD_TAG);
	}

	public static void init() {
		// 每个服务端 tick（20 次/秒）对每个维度各回调一次，下面再按 20 tick 节流成"每秒一次"
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
		// 只管灰界这一个维度
		if (!level.dimension().equals(GRAY_REALM)) {
			return;
		}
		// 节流：每 20 tick（=1 秒）处理一次，避免每 tick 都遍历实体
		if (level.getGameTime() % 20L != 0L) {
			return;
		}
		GrayRealmLayout worldLayout = layoutOf(level);
		if (worldLayout == null) {
			return; // 还没生成过区块，没有布局可用
		}

		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}
			double x = living.getX();
			double z = living.getZ();
			Holder<Biome> biome = level.getBiome(living.blockPosition());

			// ========================= 位置 A =========================
			// 所有灰界活物都会经过这里（含免疫、含边界）。
			// 想让某个效果"全灰界都吃"就加在这里，例如：
			//   living.addEffect(new MobEffectInstance(MobEffects.HUNGER, REFRESH_DURATION, 0, true, false, true));
			// =========================================================

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
				// ===================== 位置 B =========================
				// 只有"超出地图半径"的活物会到这里，想让边界额外附带效果就加在这里。
				// ======================================================
				continue;
			}

			// ② 灰界生物 / 神道领域：豁免普通灰蚀
			// ===================== 位置 C =============================
			// 这一行之后，被豁免的活物就 continue 走了；想"绕过豁免"的效果要放在它上面。
			// ==========================================================
			if (isImmune(living)) {
				continue;
			}

			// ③ 区域 / 灰雾带：普通灰蚀
			// ===================== 位置 D =============================
			// 只对"区域 + 灰雾带、且没被豁免"的活物生效。
			// ==========================================================
			int level0 = worldLayout.erosionLevel(x, z);
			living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(XishenMod.GRAY_EROSION),
					REFRESH_DURATION, level0, true, false, true));

			if (biome.is(FOG_BIOME)) {
				living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, REFRESH_DURATION, 0, true, false, true));
			}
		}
	}
}
