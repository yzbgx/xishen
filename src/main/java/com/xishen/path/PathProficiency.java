package com.xishen.path;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 职业熟练度的获取（行为 → 神道）。
 *
 * <pre>
 *   挖方块            → 力 +1
 *   收割作物          → 黄 +1      （黄 = 商人 / 农民）
 *   与村民交易        → 黄 +2      （黄 = 商人 / 农民，按交易统计增量算）
 *   击杀生物          → 兵 +3
 *   潜行击杀          → 娼 +5      （娼 = 刺客，潜行偷袭）
 *   使用末影之眼      → 卜 +3      （卜 = 占卜 / 寻路）
 *   放置画作          → 青 +3      （青 = 丹青 / 画家）
 *
 *   待接：合成→巧、附魔/书→书、酿造→巫、开箱→盗、驯养/繁殖→偶、
 *        吃东西/治疗→医、红石→弈、音符盒/唱片→戏、探索新群系→卜
 * </pre>
 */
public final class PathProficiency {
	private PathProficiency() {
	}

	/** 记录每个玩家上次的"与村民交易次数"，用来算增量 */
	private static final Map<UUID, Integer> LAST_TRADE_COUNT = new HashMap<>();

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
		// ① 挖方块 → 力；收割作物 → 黄
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			gain(serverPlayer, PathType.LI, 1);
			if (state.getBlock() instanceof CropBlock) {
				gain(serverPlayer, PathType.HUANG, 1);
			}
		});

		// ② 使用物品：末影之眼 → 卜；画 → 青
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
				var stack = player.getItemInHand(hand);
				if (stack.is(Items.ENDER_EYE)) {
					gain(serverPlayer, PathType.BU, 3);
				}
				if (stack.is(Items.PAINTING)) {
					gain(serverPlayer, PathType.QING, 3);
				}
			}
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		});

		// ③ 击杀：潜行击杀 → 娼（刺客），普通击杀 → 兵
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(damageSource.getEntity() instanceof ServerPlayer player)) {
				return;
			}
			if (!(entity instanceof LivingEntity)) {
				return;
			}
			if (player.isCrouching()) {
				gain(player, PathType.CHANG, 5);
			} else {
				gain(player, PathType.BING, 3);
			}
		});

		// ④ 每秒检查"与村民交易次数"的增量 → 黄
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getGameTime() % 20L != 0L) {
				return;
			}
			for (ServerPlayer player : world.players()) {
				int now = player.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
				Integer last = LAST_TRADE_COUNT.get(player.getUUID());
				if (last == null) {
					LAST_TRADE_COUNT.put(player.getUUID(), now);
				} else if (now > last) {
					LAST_TRADE_COUNT.put(player.getUUID(), now);
					gain(player, PathType.HUANG, 2 * (now - last));
				}
			}
		});

		// ⑤ 玩家离线时清掉交易计数缓存
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUUID()));
	}

	/** 玩家离线时清掉交易计数缓存 */
	public static void forget(UUID uuid) {
		LAST_TRADE_COUNT.remove(uuid);
	}
}
