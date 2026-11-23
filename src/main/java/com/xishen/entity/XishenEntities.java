package com.xishen.entity;

import com.xishen.XishenMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * 实体注册。目前只有一个：血肉船。
 */
public final class XishenEntities {
	private XishenEntities() {
	}

	public static final EntityType<FleshBoat> FLESH_BOAT = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			XishenMod.id("flesh_boat"),
			EntityType.Builder.<FleshBoat>of(FleshBoat::new, MobCategory.MISC)
					.sized(1.375F, 0.5625F)
					.clientTrackingRange(10)
					.build("flesh_boat"));

	public static void register() {
		// 触发类加载，静态字段即完成注册
	}
}
