package com.xishen.path;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 职业熟练度的获取（行为 → 神道）。数值都是框架占位，规则定了再调。
 *
 * <pre>
 *   挖方块            → 力 +1
 *   收割作物          → 黄 +1
 *   与村民交易        → 黄 +2      （按"与村民交易次数"统计的增量）
 *   击杀生物          → 兵 +3
 *   潜行击杀          → 娼 +5      （娼 = 刺客）
 *   合成物品          → 巧 +2      （ResultSlot mixin）
 *   放置方块          → 巧 +1
 *   放置红石元件      → 弈 +1
 *   使用末影之眼      → 卜 +3
 *   首次到新群系      → 卜 +2
 *   放置画作          → 青 +3      （青 = 丹青 / 画家）
 *   附魔台            → 书 +2
 *   酿造台            → 巫 +2
 *   开箱子 / 木桶     → 盗 +1
 *   驯服动物          → 偶 +2
 *   吃东西            → 医 +2
 *   音符盒 / 唱片机   → 戏 +2
 * </pre>
 */
public final class PathProficiency {
	private PathProficiency() {
	}

	private static final Map<UUID, Integer> LAST_TRADE_COUNT = new HashMap<>();
	private static final Map<UUID, String> LAST_BIOME = new HashMap<>();

	/** 红石元件（放下去算"弈"） */
	private static final Set<net.minecraft.world.level.block.Block> REDSTONE_BLOCKS = Set.of(
			Blocks.REDSTONE_WIRE, Blocks.REPEATER, Blocks.COMPARATOR, Blocks.PISTON, Blocks.STICKY_PISTON,
			Blocks.OBSERVER, Blocks.LEVER, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_LAMP,
			Blocks.DAYLIGHT_DETECTOR, Blocks.TRIPWIRE_HOOK, Blocks.TARGET, Blocks.SCULK_SENSOR);

	/** 服务端：给玩家某条神道加熟练度（超过上限会截断） */
	public static void gain(ServerPlayer player, PathType type, int amount) {
		if (amount <= 0) {
			return;
		}
		PathData data = PathAttachments.get(player);
		PathData updated = data.addProficiency(type, amount);
		if (!updated.equals(data)) {
			PathAttachments.set(player, updated);
			PathFeedback.onProficiencyChanged(player, type, data.proficiency(type), updated.proficiency(type));
		}
	}

	public static void register() {
		// ① 挖方块 → 力；收割作物 → 黄
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			gain(serverPlayer, PathType.LI, PathValues.LI_PER_BLOCK);
			if (state.getBlock() instanceof CropBlock) {
				gain(serverPlayer, PathType.HUANG, PathValues.HUANG_PER_CROP);
			}
		});

		// ② 使用物品：末影之眼 → 卜；画 → 青；吃东西 → 医
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
				ItemStack stack = player.getItemInHand(hand);
				if (stack.is(Items.ENDER_EYE)) {
					gain(serverPlayer, PathType.BU, PathValues.BU_PER_EYE);
				}
				if (stack.is(Items.PAINTING)) {
					gain(serverPlayer, PathType.QING, PathValues.QING_PER_PAINTING);
				}
				if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
					gain(serverPlayer, PathType.YI, PathValues.YI_PER_EAT);
				}
			}
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		});

		// ③ 使用方块：附魔台 → 书；酿造台 → 巫；箱子/木桶 → 盗；音符盒/唱片机 → 戏
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hitResult.getBlockPos();
			var state = world.getBlockState(pos);

			if (state.getBlock() instanceof EnchantingTableBlock) {
				gain(serverPlayer, PathType.SHU, PathValues.SHU_PER_ENCHANT);
			}
			if (state.getBlock() instanceof BrewingStandBlock) {
				gain(serverPlayer, PathType.WU, PathValues.WU_PER_BREW);
			}
			if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)
					|| state.is(Blocks.SHULKER_BOX)) {
				gain(serverPlayer, PathType.DAO, PathValues.DAO_PER_CHEST);
			}
			if (state.is(Blocks.NOTE_BLOCK) || state.is(Blocks.JUKEBOX)) {
				gain(serverPlayer, PathType.XI, PathValues.XI_PER_MUSIC);
			}

			// 放置方块 → 巧；红石元件额外 → 弈
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof BlockItem blockItem) {
				gain(serverPlayer, PathType.QIAO, PathValues.QIAO_PER_PLACE);
				if (REDSTONE_BLOCKS.contains(blockItem.getBlock())) {
					gain(serverPlayer, PathType.YIX, PathValues.YIX_PER_REDSTONE);
				}
			}
			return InteractionResult.PASS;
		});

		// ④ 使用实体：驯服动物 → 偶
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClientSide && player instanceof ServerPlayer serverPlayer
					&& entity instanceof TamableAnimal tameable && !tameable.isTame()
					&& !player.getItemInHand(hand).isEmpty()) {
				gain(serverPlayer, PathType.OU, PathValues.OU_PER_TAME);
			}
			return InteractionResult.PASS;
		});

		// ⑤ 击杀：潜行击杀 → 娼（刺客），普通击杀 → 兵
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(damageSource.getEntity() instanceof ServerPlayer player) || !(entity instanceof LivingEntity)) {
				return;
			}
			if (player.isCrouching()) {
				gain(player, PathType.CHANG, PathValues.CHANG_PER_SNEAK_KILL);
			} else {
				gain(player, PathType.BING, PathValues.BING_PER_KILL);
			}
		});

		// ⑥ 每秒：与村民交易次数增量 → 黄；换群系 → 卜
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getGameTime() % 20L != 0L) {
				return;
			}
			for (ServerPlayer player : world.players()) {
				int trades = player.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
				Integer lastTrades = LAST_TRADE_COUNT.get(player.getUUID());
				if (lastTrades == null) {
					LAST_TRADE_COUNT.put(player.getUUID(), trades);
				} else if (trades > lastTrades) {
					LAST_TRADE_COUNT.put(player.getUUID(), trades);
					gain(player, PathType.HUANG, PathValues.HUANG_PER_TRADE * (trades - lastTrades));
				}

				ResourceKey<Biome> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
				String biome = biomeKey == null ? "" : biomeKey.location().toString();
				String lastBiome = LAST_BIOME.get(player.getUUID());
				if (lastBiome == null) {
					LAST_BIOME.put(player.getUUID(), biome);
				} else if (!biome.isEmpty() && !biome.equals(lastBiome)) {
					LAST_BIOME.put(player.getUUID(), biome);
					gain(player, PathType.BU, PathValues.BU_PER_BIOME);
				}
			}
		});

		// ⑦ 断线清理缓存
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer().getUUID()));
	}

	public static void forget(UUID uuid) {
		LAST_TRADE_COUNT.remove(uuid);
		LAST_BIOME.remove(uuid);
	}
}
