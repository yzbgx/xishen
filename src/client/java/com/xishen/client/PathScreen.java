package com.xishen.client;

import com.xishen.path.PathAttachments;
import com.xishen.path.PathData;
import com.xishen.path.PathNetworking;
import com.xishen.path.PathType;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 神道界面（参考 Reskillable 的技能页 + 饰品栏 mod 的入口按钮）。
 *
 * <p>14 条神道，每条显示：代表色方块 + 名字 + **职业熟练度** + 状态（未激活 / 第 N 阶）+ 「+」按钮。
 *
 * <ul>
 *   <li>熟练度 0~99：显示「未激活」，进度条按 0~100 走</li>
 *   <li>熟练度 ≥100：激活，显示「第 N 阶 / 9」，进度条按当前阶内的进度走</li>
 *   <li>点「+」= 消耗 1 点加点，熟练度 +{@link PathNetworking#PROFICIENCY_PER_POINT}</li>
 * </ul>
 */
public class PathScreen extends Screen {
	private static final int PANEL_W = 344;
	private static final int PANEL_H = 246;
	private static final int ROW_H = 30;
	private static final int ROWS_PER_COLUMN = 7;
	private static final int COLUMN_W = 164;

	private final Map<PathType, Button> plusButtons = new EnumMap<>(PathType.class);

	public PathScreen() {
		super(Component.translatable("gui.xishen.paths.title"));
	}

	@Override
	protected void init() {
		this.plusButtons.clear();
		int left = (this.width - PANEL_W) / 2;
		int top = (this.height - PANEL_H) / 2;

		for (PathType type : PathType.ALL) {
			int index = type.ordinal();
			int column = index / ROWS_PER_COLUMN;
			int row = index % ROWS_PER_COLUMN;
			int x = left + 10 + column * COLUMN_W;
			int y = top + 30 + row * ROW_H;

			Button plus = Button.builder(Component.literal("+"), button ->
					ClientPlayNetworking.send(new PathNetworking.SpendPoint(type.id())))
					.bounds(x + 136, y + 5, 18, 18)
					.build();
			this.plusButtons.put(type, plus);
			this.addRenderableWidget(plus);
		}

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
				.bounds(left + PANEL_W - 74, top + PANEL_H - 26, 64, 18).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);

		int left = (this.width - PANEL_W) / 2;
		int top = (this.height - PANEL_H) / 2;

		// 面板底 + 边框
		graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0101014);
		graphics.fill(left, top, left + PANEL_W, top + 1, 0xFF6A6A6A);
		graphics.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF6A6A6A);
		graphics.fill(left, top, left + 1, top + PANEL_H, 0xFF6A6A6A);
		graphics.fill(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, 0xFF6A6A6A);

		graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xFFFFFF);

		PathData data = clientData();
		PathType hovered = null;
		String points = Component.translatable("gui.xishen.paths.points", data.points()).getString();
		graphics.drawString(this.font, points, left + 10, top + PANEL_H - 20, 0xC8C8C8, false);

		for (PathType type : PathType.ALL) {
			int index = type.ordinal();
			int column = index / ROWS_PER_COLUMN;
			int row = index % ROWS_PER_COLUMN;
			int x = left + 10 + column * COLUMN_W;
			int y = top + 30 + row * ROW_H;

			int proficiency = data.proficiency(type);
			int level = typeLevel(data, type);
			boolean activated = proficiency >= PathType.ACTIVATION_PROFICIENCY;

			// 代表色方块 + 汉字（激活后更亮）
			int color = type.color();
			if (!activated) {
				color = darken(color, 0.45F);
			}
			graphics.fill(x, y, x + 20, y + 20, 0xFF000000 | color);
			graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF000000 | darken(color, 0.55F));
			graphics.drawCenteredString(this.font, type.zhName(), x + 10, y + 6, 0xFFFFFF);

			// 名字
			graphics.drawString(this.font, Component.translatable(type.translationKey()), x + 26, y + 2, 0xFFFFFF, false);

			// 状态：未激活 / 第 N 阶
			String status = activated
					? Component.translatable("gui.xishen.paths.tier", level, PathType.MAX_LEVEL).getString()
					: Component.translatable("gui.xishen.paths.inactive", proficiency, PathType.ACTIVATION_PROFICIENCY).getString();
			graphics.drawString(this.font, status, x + 26, y + 12, activated ? 0xE0E0A0 : 0x909090, false);

			// 进度条：未激活按 0~100，已激活按当前阶内 0~100
			int barX = x + 26;
			int barY = y + 22;
			int barW = 108;
			int within = activated
					? (proficiency % PathType.PROFICIENCY_PER_LEVEL)
					: proficiency;
			graphics.fill(barX, barY, barX + barW, barY + 2, 0xFF2A2A2A);
			int filled = (int) Math.round(barW * (within / (double) PathType.PROFICIENCY_PER_LEVEL));
			if (filled > 0) {
				graphics.fill(barX, barY, barX + filled, barY + 2, 0xFF000000 | type.color());
			}

			if (mouseX >= x && mouseX < x + 158 && mouseY >= y && mouseY < y + ROW_H - 4) {
				hovered = type;
			}

			Button plus = this.plusButtons.get(type);
			if (plus != null) {
				plus.active = data.points() > 0 && proficiency < PathType.MAX_PROFICIENCY;
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);

		if (hovered != null && plusTooltipExpired(hovered, data)) {
			graphics.renderComponentTooltip(this.font, List.of(
					Component.translatable(hovered.translationKey()),
					Component.translatable("gui.xishen.paths.source", Component.translatable(hovered.sourceKey()))
							.withStyle(net.minecraft.ChatFormatting.GRAY),
					Component.translatable("gui.xishen.paths.prof", data.proficiency(hovered),
							PathType.MAX_PROFICIENCY).withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
			), mouseX, mouseY);
		}
	}

	/** 这里只是占位，方便以后加"悬停条件"，现在始终显示 */
	private static boolean plusTooltipExpired(PathType type, PathData data) {
		return true;
	}

	private static int typeLevel(PathData data, PathType type) {
		return data.level(type);
	}

	private static int darken(int color, float factor) {
		int r = (int) (((color >> 16) & 0xFF) * factor);
		int g = (int) (((color >> 8) & 0xFF) * factor);
		int b = (int) ((color & 0xFF) * factor);
		return (r << 16) | (g << 8) | b;
	}

	private PathData clientData() {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return PathData.empty();
		}
		return player.getAttachedOrCreate(PathAttachments.PATH_DATA);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
