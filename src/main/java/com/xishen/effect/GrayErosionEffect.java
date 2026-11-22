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
 * 灰蚀 —— 灰界对活物的持续侵蚀。
 *
 * 不是原版凋零/中毒：伤害走自定义伤害类型 xishen:gray_erosion（见 data/xishen/damage_type/gray_erosion.json），
 * 死亡信息也是独立的（"xxx 被灰界侵蚀"）。
 *
 * 伤害频率固定 2 秒一次，具体数值由"等级（amplifier）"决定：
 * 等级越高，单次伤害越高 —— 这个等级是由 GrayRealmTicker 按"离实体区域多远"实时算出来的。
 */
public class GrayErosionEffect extends MobEffect {
	/** 自定义伤害类型：xishen:gray_erosion */
	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, XishenMod.id("gray_erosion"));

	/** 每隔多少 tick 结算一次伤害（40 tick = 2 秒） */
	private static final int DAMAGE_INTERVAL = 40;
	/** 0 级时每次伤害多少点（1 点 = 半颗心） */
	private static final float BASE_DAMAGE = 1.0F;

	public GrayErosionEffect() {
		super(MobEffectCategory.HARMFUL, 0x3A3F45);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		// 只在服务端结算，客户端不造成伤害
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
