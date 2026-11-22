package com.xishen.effect;

import com.xishen.XishenMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「灰蚀：边界」—— 灰界最外圈（超出地图半径）的强制侵蚀。
 *
 * 与普通灰蚀的区别：
 *   1. 无视一切豁免（灰界生物、神道领域免疫都挡不住），任何活物在边界都会被挂上；
 *   2. 伤害更高，并且随"离边界多远"继续递增（参考虚空）。
 *
 * 伤害走独立伤害类型 xishen:gray_erosion_edge，死亡提示也是独立的。
 */
public class GrayErosionEdgeEffect extends MobEffect {
	public static final ResourceKey<DamageType> DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, XishenMod.id("gray_erosion_edge"));

	private static final int DAMAGE_INTERVAL = 40;   // 每 2 秒
	private static final float BASE_DAMAGE = 2.0F;   // 基础 2 点，比普通灰蚀高一倍

	public GrayErosionEdgeEffect() {
		super(MobEffectCategory.HARMFUL, 0x101010);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return true;
		}
		if (entity.tickCount % DAMAGE_INTERVAL != 0) {
			return true;
		}
		DamageSource source = new DamageSource(
				serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
		entity.hurt(source, BASE_DAMAGE * (1 + amplifier));
		return true;
	}
}
