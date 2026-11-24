package com.xishen.client;

import com.xishen.path.PathAttachments;
import com.xishen.path.PathData;
import com.xishen.path.PathType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 神道界面（参考 Reskillable 的技能页 + 饰品栏 mod 的入口按钮）。
 *
 * <p>布局：一个面板，14 条神道分两列（每列 7 条），每条显示：
 * 代表色方块 + 神道名 + 当前等级 + 「+」按钮。
 *
 * <p>现在只是框架：加点的具体方式（升级、消耗点数、解锁条件）还没定，
 * 所以「+」按钮暂时是灰的，点不动；数据已经能存能同步了。
 */
public class PathScreen extends Screen {
	private static final int PANEL_W = 344;
	private static final int PANEL_H = 246;
	private static final int ROW_H = 30;
	private static final int ROWS_PER_COLUMN = 7;
	private static final int COLUMN_W = 164;

	private final List<Button> plusButtons = new ArrayList<>();

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

			Button plus = Button.builder(Component.literal("+"), button -> {
				// 加点方式待定：这里先只打印日志，等规则定下来再实现
			}).bounds(x + 136, y + 5, 18, 18).build();
			plus.active = false; // 规则未定，先禁用
			this.plusButtons.add(plus);
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
		String points = Component.translatable("gui.xishen.paths.points", data.points()).getString();
		graphics.drawString(this.font, points, left + 10, top + PANEL_H - 20, 0xC8C8C8, false);

		for (PathType type : PathType.ALL) {
			int index = type.ordinal();
			int column = index / ROWS_PER_COLUMN;
			int row = index % ROWS_PER_COLUMN;
			int x = left + 10 + column * COLUMN_W;
			int y = top + 30 + row * ROW_H;

			// 代表色方块 + 汉字
			graphics.fill(x, y, x + 20, y + 20, 0xFF000000 | type.color());
			graphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF000000 | darken(type.color()));
			graphics.drawCenteredString(this.font, type.zhName(), x + 10, y + 6, 0xFFFFFF);

			// 名字
			graphics.drawString(this.font, Component.translatable(type.translationKey()), x + 26, y + 3, 0xFFFFFF, false);
			// 等级
			int level = data.level(type);
			String levelText = Component.translatable("gui.xishen.paths.level", level, PathType.MAX_LEVEL).getString();
			graphics.drawString(this.font, levelText, x + 26, y + 14, 0xA0A0A0, false);

			// 等级进度条
			int barX = x + 26;
			int barY = y + 24;
			int barW = 108;
			graphics.fill(barX, barY, barX + barW, barY + 2, 0xFF2A2A2A);
			int filled = (int) Math.round(barW * (level / (double) PathType.MAX_LEVEL));
			if (filled > 0) {
				graphics.fill(barX, barY, barX + filled, barY + 2, 0xFF000000 | type.color());
			}
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private static int darken(int color) {
		int r = (int) (((color >> 16) & 0xFF) * 0.55);
		int g = (int) (((color >> 8) & 0xFF) * 0.55);
		int b = (int) ((color & 0xFF) * 0.55);
		return (r << 16) | (g << 8) | b;
	}

	private PathData clientData() {
		Player player = net.minecraft.client.Minecraft.getInstance().player;
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
