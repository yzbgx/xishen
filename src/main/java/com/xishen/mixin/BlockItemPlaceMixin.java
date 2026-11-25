package com.xishen.mixin;

import com.xishen.path.PathProficiency;
import com.xishen.path.PathType;
import com.xishen.path.PathValues;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * 「真正放下方块」才加巧/弈的熟练度。
 *
 * <p>之前挂在"右键方块"的回调上，对着空气右键也会加；现在改成挂 {@link BlockItem#place}
 * 的返回值：只有放置成功（返回结果算"生效"）才加。
 */
@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {
	private static final Set<net.minecraft.world.level.block.Block> REDSTONE_BLOCKS = Set.of(
			Blocks.REDSTONE_WIRE, Blocks.REPEATER, Blocks.COMPARATOR, Blocks.PISTON, Blocks.STICKY_PISTON,
			Blocks.OBSERVER, Blocks.LEVER, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_LAMP,
			Blocks.DAYLIGHT_DETECTOR, Blocks.TRIPWIRE_HOOK, Blocks.TARGET, Blocks.SCULK_SENSOR);

	@Inject(method = "place", at = @At("RETURN"))
	private void xishen$afterPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return;
		}
		InteractionResult result = cir.getReturnValue();
		if (result == null || !result.consumesAction()) {
			return;
		}
		PathProficiency.gain(player, PathType.QIAO, PathValues.QIAO_PER_PLACE);
		if (REDSTONE_BLOCKS.contains(((BlockItem) (Object) this).getBlock())) {
			PathProficiency.gain(player, PathType.YIX, PathValues.YIX_PER_REDSTONE);
		}
	}
}
