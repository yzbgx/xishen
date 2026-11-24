package com.xishen.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.xishen.path.PathCommands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * 灰界相关指令：
 *
 *   /xishen regions                 列出当前世界所有区域（id + 中心坐标 + 半径）
 *   /xishen tp &lt;区域id&gt;         传送到该区域的**正中心**（自动落地）
 *
 * 区域中心来自 GrayRealmLayout（每个世界随机生成的那份布局），所以永远对准真正的圆心。
 */
public final class GrayRealmCommands {
	private GrayRealmCommands() {
	}

	private static final SuggestionProvider<CommandSourceStack> REGION_SUGGESTIONS = (context, builder) -> {
		GrayRealmLayout layout = layoutOf(context.getSource().getServer().getLevel(GrayRealmTicker.GRAY_REALM));
		if (layout == null) {
			return builder.buildFuture();
		}
		return SharedSuggestionProvider.suggest(layout.regions().stream().map(GrayRealmLayout.PlacedRegion::id).toList(), builder);
	};

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("xishen")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("regions").executes(GrayRealmCommands::listRegions))
				.then(PathCommands.branch())
				.then(Commands.literal("tp")
						.then(Commands.argument("region", StringArgumentType.word())
								.suggests(REGION_SUGGESTIONS)
								.executes(GrayRealmCommands::teleport))));
	}

	private static GrayRealmLayout layoutOf(ServerLevel level) {
		if (level != null
				&& level.getChunkSource().getGenerator().getBiomeSource() instanceof GrayRealmBiomeSource source) {
			return source.layout();
		}
		return null;
	}

	private static int listRegions(CommandContext<CommandSourceStack> context) {
		GrayRealmLayout layout = layoutOf(context.getSource().getServer().getLevel(GrayRealmTicker.GRAY_REALM));
		if (layout == null) {
			context.getSource().sendFailure(Component.literal("Gray Realm has no generated chunks yet; enter it once first."));
			return 0;
		}
		List<GrayRealmLayout.PlacedRegion> regions = layout.regions();
		context.getSource().sendSuccess(() -> Component.literal("Gray Realm regions (" + regions.size() + "):"), false);
		for (GrayRealmLayout.PlacedRegion region : regions) {
			context.getSource().sendSuccess(() -> Component.literal(String.format(
					"  %s center=(%d,%d) radius=%d",
					region.id(), (int) region.centerX(), (int) region.centerZ(), (int) region.radius())), false);
		}
		return regions.size();
	}

	private static int teleport(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		String regionId = StringArgumentType.getString(context, "region");
		ServerLevel level = context.getSource().getServer().getLevel(GrayRealmTicker.GRAY_REALM);
		if (level == null) {
			context.getSource().sendFailure(Component.literal("Gray Realm dimension not found."));
			return 0;
		}
		GrayRealmLayout layout = layoutOf(level);
		if (layout == null) {
			context.getSource().sendFailure(Component.literal("Gray Realm has no generated chunks yet."));
			return 0;
		}
		GrayRealmLayout.PlacedRegion region = layout.regionById(regionId);
		if (region == null) {
			context.getSource().sendFailure(Component.literal("No region named " + regionId + ". Use /xishen regions."));
			return 0;
		}

		ServerPlayer player = context.getSource().getPlayerOrException();
		int x = (int) region.centerX();
		int z = (int) region.centerZ();
		level.getChunk(x >> 4, z >> 4);   // 确保中心区块已生成，才能取到正确高度
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 2;

		player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
		context.getSource().sendSuccess(() -> Component.literal(String.format(
				"Teleported to center of %s at (%d, %d, %d)", region.id(), x, y, z)), false);
		return 1;
	}
}
