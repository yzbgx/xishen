package com.xishen.path;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 职业熟练度的获取。
 *
 * <p>行为 → 神道 的对应关系（现在只接了几个好接的，其余先留空等以后接）：
 *
 * <pre>
 *   挖方块          → 力 +1
 *   挖矿石          → 黄 +2
 *   挖原木/树叶     → 青 +1
 *   击杀生物        → 兵 +3
 *   合成物品        → 巧 +2   （待接）
 *   放方块建造      → 巧 +1
 *   附魔 / 书       → 书 +3   （待接）
 *   酿造台 / 药水   → 巫 +3   （待接）
 *   交易            → 娼 +2   （待接）
 *   开箱子 / 潜行   → 盗 +1   （待接）
 *   探索新群系      → 卜 +2   （待接）
 *   驯养 / 繁殖     → 偶 +2   （待接）
 *   吃东西 / 治疗   → 医 +2   （待接）
 *   红石元件        → 弈 +2   （待接）
 *   音符盒 / 唱片   → 戏 +2   （待接）
 * </pre>
 *
 * 数值都是框架占位，等规则定了再调。
 */
public final class PathProficiency {
	private PathProficiency() {
	}

	/** 服务端：给玩家某条神道加熟练度（超过 900 会截断） */
	public static void gain(ServerPlayer player, PathType type, int amount) {
		if (amount <= 0) {
			return;
		}
		PathData data = PathAttachments.get(player);
		PathData updated = data.addProficiency(type, amount);
		if (!updated.equals(data)) {
			PathAttachments.set(player, updated);
		}
	}

	public static void register() {
		// ① 挖方块：力 / 黄（矿石）/ 青（木头）
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			gain(serverPlayer, PathType.LI, 1);
			if (isOre(state)) {
				gain(serverPlayer, PathType.HUANG, 2);
			}
			if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
				gain(serverPlayer, PathType.QING, 1);
			}
		});

		// ② 击杀生物：兵
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(damageSource.getEntity() instanceof ServerPlayer player)) {
				return;
			}
			if (entity instanceof LivingEntity) {
				gain(player, PathType.BING, 3);
			}
		});
	}

	private static boolean isOre(BlockState state) {
		return state.is(BlockTags.COAL_ORES)
				|| state.is(BlockTags.IRON_ORES)
				|| state.is(BlockTags.COPPER_ORES)
				|| state.is(BlockTags.GOLD_ORES)
				|| state.is(BlockTags.REDSTONE_ORES)
				|| state.is(BlockTags.LAPIS_ORES)
				|| state.is(BlockTags.DIAMOND_ORES)
				|| state.is(BlockTags.EMERALD_ORES);
	}
}
