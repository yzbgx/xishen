package com.xishen.item;

import com.xishen.entity.FleshBoat;
import com.xishen.entity.XishenEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 血肉船的物品：逻辑照原版 {@code BoatItem} 抄了一遍，只是生成的是我们的 {@link FleshBoat}。
 */
public class FleshBoatItem extends Item {
	public FleshBoatItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResultHolder.pass(stack);
		}
		Vec3 location = hit.getLocation();
		FleshBoat boat = new FleshBoat(XishenEntities.FLESH_BOAT, level);
		boat.setPos(location.x, location.y, location.z);
		boat.setYRot(player.getYRot());
		if (!level.noCollision(boat, boat.getBoundingBox().inflate(-0.1D))) {
			return InteractionResultHolder.fail(stack);
		}
		if (!level.isClientSide) {
			level.addFreshEntity(boat);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(location));
			player.awardStat(Stats.ITEM_USED.get(this));
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		stack.shrink(1);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
