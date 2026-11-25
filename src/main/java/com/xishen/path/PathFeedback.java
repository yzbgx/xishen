package com.xishen.path;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 神道进阶的反馈：熟练度刚好到 100（激活）或每跨过 100（升阶）时，
 * 给玩家一条聊天提示 + 一个音效。
 */
public final class PathFeedback {
	private PathFeedback() {
	}

	/** 在熟练度变化前后对比，必要时发提示 */
	public static void onProficiencyChanged(ServerPlayer player, PathType type, int before, int after) {
		int oldLevel = PathType.levelFor(before);
		int newLevel = PathType.levelFor(after);
		if (newLevel == oldLevel) {
			return;
		}

		// 先把"效果"钩子留出来（内容以后再填）
		PathEffects.onTierUp(player, type, newLevel);
		PathEffects.applyPassive(player);

		if (oldLevel == 0 && newLevel >= 1) {
			// 刚激活
			player.sendSystemMessage(Component.translatable("message.xishen.path.activated", type.zhName())
					.withStyle(ChatFormatting.GOLD));
			player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
		} else if (newLevel > oldLevel) {
			// 升阶
			player.sendSystemMessage(Component.translatable("message.xishen.path.tier_up", type.zhName(), newLevel)
					.withStyle(ChatFormatting.YELLOW));
			player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.2F);
		}
	}
}
