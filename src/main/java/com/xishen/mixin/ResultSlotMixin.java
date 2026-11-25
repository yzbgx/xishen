package com.xishen.mixin;

import com.xishen.path.PathProficiency;
import com.xishen.path.PathValues;
import com.xishen.path.PathType;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「合成完成」钩子：玩家从合成结果槽里拿走成品时，给巧神道加熟练度。
 *
 * <p>Fabric 没有现成的"合成完成"事件，所以直接挂在 {@link ResultSlot#onTake} 上。
 */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void xishen$onCraft(Player player, ItemStack stack, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
			PathProficiency.gain(serverPlayer, PathType.QIAO, PathValues.QIAO_PER_CRAFT);
		}
	}
}
