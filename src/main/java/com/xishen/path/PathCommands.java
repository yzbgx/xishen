package com.xishen.path;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * 神道相关指令（目前用于调试/测试，`/xishen path ...`）：
 *
 * <pre>
 * /xishen path info                 查看自己的 14 条神道等级和剩余点数
 * /xishen path points &lt;n&gt;         直接设置剩余点数
 * /xishen path add &lt;神道&gt; [级数]   给某条神道加等级（默认 +1）
 * /xishen path reset                全部清零
 * </pre>
 */
public final class PathCommands {
	private PathCommands() {
	}

	private static final SuggestionProvider<CommandSourceStack> PATH_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(PathType.ALL).map(PathType::id).toList(), builder);

	public static LiteralArgumentBuilder<CommandSourceStack> branch() {
		return Commands.literal("path")
				.then(Commands.literal("info").executes(PathCommands::info))
				.then(Commands.literal("points")
						.then(Commands.argument("n", IntegerArgumentType.integer(0, 9999))
								.executes(PathCommands::setPoints)))
				.then(Commands.literal("add")
						.then(Commands.argument("path", StringArgumentType.word())
								.suggests(PATH_SUGGESTIONS)
								.executes(context -> addLevel(context, 1))
								.then(Commands.argument("n", IntegerArgumentType.integer(1, PathType.MAX_LEVEL))
										.executes(context -> addLevel(context, IntegerArgumentType.getInteger(context, "n"))))))
				.then(Commands.literal("reset").executes(PathCommands::reset));
	}

	private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		PathData data = PathAttachments.get(player);
		StringBuilder sb = new StringBuilder("Paths (points=" + data.points() + "):");
		for (PathType type : PathType.ALL) {
			sb.append(' ').append(type.id()).append('=').append(data.level(type));
		}
		String message = sb.toString();
		context.getSource().sendSuccess(() -> Component.literal(message), false);
		return data.points();
	}

	private static int setPoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int n = IntegerArgumentType.getInteger(context, "n");
		PathData data = PathAttachments.get(player).withPoints(n);
		PathAttachments.set(player, data);
		context.getSource().sendSuccess(() -> Component.literal("Points set to " + n), false);
		return n;
	}

	private static int addLevel(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String id = StringArgumentType.getString(context, "path");
		PathType type = PathType.byId(id);
		if (type == null) {
			context.getSource().sendFailure(Component.literal("Unknown path: " + id));
			return 0;
		}
		PathData data = PathAttachments.get(player);
		int old = data.level(type);
		int updated = Math.min(PathType.MAX_LEVEL, old + amount);
		PathAttachments.set(player, data.withLevel(type, updated));
		context.getSource().sendSuccess(() -> Component.literal(
				"Path " + id + ": " + old + " -> " + updated), false);
		return updated;
	}

	private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		PathAttachments.set(player, PathData.empty());
		context.getSource().sendSuccess(() -> Component.literal("Paths reset"), false);
		return 1;
	}
}
