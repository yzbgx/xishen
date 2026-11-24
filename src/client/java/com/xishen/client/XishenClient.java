package com.xishen.client;

import com.xishen.entity.XishenEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

/**
 * 仅客户端的入口：渲染、按键绑定、HUD 之类只存在于客户端的逻辑写在这里。
 * 这里只能 import 客户端类，不能引用仅服务端存在的类。
 */
public class XishenClient implements ClientModInitializer {
	/** 打开神道界面的按键（默认 K） */
	public static final KeyMapping OPEN_PATHS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.xishen.paths", GLFW.GLFW_KEY_K, "category.xishen"));

	@Override
	public void onInitializeClient() {
		// 血肉船的渲染器
		EntityRendererRegistry.register(XishenEntities.FLESH_BOAT, FleshBoatRenderer::new);

		// 按键打开神道界面
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_PATHS.consumeClick()) {
				if (client.player != null) {
					client.setScreen(new PathScreen());
				}
			}
		});

		// 背包界面左侧加一个「神道」按钮（类似饰品栏 mod 的入口）
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof InventoryScreen)) {
				return;
			}
			// 背包 GUI 是 176x166、居中显示；按钮放在它左侧
			int guiLeft = (screen.width - 176) / 2;
			int guiTop = (screen.height - 166) / 2;
			Screens.getButtons(screen).add(Button.builder(Component.translatable("gui.xishen.paths.button"), button -> {
				client.setScreen(new PathScreen());
			}).bounds(guiLeft - 26, guiTop + 4, 24, 20).build());
		});
	}
}
