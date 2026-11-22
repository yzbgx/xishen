package com.xishen;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 戏神（Xishen）模组主入口 —— 服务端 + 客户端都会加载。
 *
 * 现在这里是空壳，只保留了模组 id、日志器和 id() 工具方法。
 * 之后要加的物品 / 方块 / 事件都写在这个类的 onInitialize() 里（或另建类，在这里调用）。
 */
public class XishenMod implements ModInitializer {
	/** 模组 id，和 fabric.mod.json 里的 "id"、资源目录 assets/xishen 保持一致。 */
	public static final String MOD_ID = "xishen";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("戏神（xishen）已加载");
	}

	/** 生成 "xishen:xxx" 这样的资源位置，注册物品模型、贴图、合成表时常用。 */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
