package com.xishen.entity;

import com.xishen.item.XishenItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 血肉船：直接继承原版 {@link Boat}，所以划水、上船、碰撞、破坏掉落这些行为全都和原版船一样，
 * 只有"掉什么"换成了血肉船物品。渲染见客户端 {@code FleshBoatRenderer}（用血肉贴图）。
 */
public class FleshBoat extends Boat {
	public FleshBoat(EntityType<? extends Boat> type, Level level) {
		super(type, level);
	}

	@Override
	public Item getDropItem() {
		return XishenItems.FLESH_BOAT;
	}
}
