package com.xishen;

import com.xishen.block.XishenBlocks;
import com.xishen.effect.GrayErosionEdgeEffect;
import com.xishen.effect.GrayErosionEffect;
import com.xishen.world.GrayRealmBiomeSource;
import com.xishen.world.GrayRealmCommands;
import com.xishen.world.GrayRealmChunkGenerator;
import com.xishen.world.GrayRealmTicker;

import net.fabricmc.api.ModInitializer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 戏神（Xishen）模组主入口 —— 服务端 + 客户端都会加载。
 *
 * 目前注册的东西：
 *  - 状态效果「灰蚀」（xishen:gray_erosion）：灰界对活物的持续侵蚀，伤害走自定义伤害类型
 *  - 灰界维度（xishen:gray_realm，纯数据包，见 data/xishen/dimension 等）
 *  - 灰界环境驱动（GrayRealmTicker）：每秒按坐标给活物挂灰蚀 / 失明 / 黑暗
 */
public class XishenMod implements ModInitializer {
	/** 模组 id，和 fabric.mod.json 里的 "id"、资源目录 assets/xishen 保持一致。 */
	public static final String MOD_ID = "xishen";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** 状态效果：灰蚀 */
	public static final GrayErosionEffect GRAY_EROSION = new GrayErosionEffect();

	/** 状态效果：灰蚀：边界（无视豁免，强制侵蚀） */
	public static final GrayErosionEdgeEffect GRAY_EROSION_EDGE = new GrayErosionEdgeEffect();

	@Override
	public void onInitialize() {
		// 注册状态效果；显示名在 assets/xishen/lang 里（effect.xishen.gray_erosion）
		Registry.register(BuiltInRegistries.MOB_EFFECT, id("gray_erosion"), GRAY_EROSION);
		Registry.register(BuiltInRegistries.MOB_EFFECT, id("gray_erosion_edge"), GRAY_EROSION_EDGE);

		// 注册灰界专用 biome source：按坐标划分 区域 / 常规灰雾带 / 边界灰雾带
		Registry.register(BuiltInRegistries.BIOME_SOURCE, id("gray_realm"), GrayRealmBiomeSource.CODEC);
		// 注册灰界地形生成器：按区域后处理地形（禁忌之海=海，灰雾带=干涸荒原）
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("gray_realm"), GrayRealmChunkGenerator.CODEC);

		// 注册灰界专属方块（灰土 / 灰草方块）
		XishenBlocks.register();

		// 注册 /xishen 指令（regions 列表、tp 传送到区域正中心）
		GrayRealmCommands.register();

		// 灰界环境驱动：每秒扫描灰界里的活物并挂效果
		GrayRealmTicker.init();

		LOGGER.info("戏神（xishen）已加载");
	}

	/** 生成 "xishen:xxx" 这样的资源位置，注册物品模型、贴图、合成表时常用。 */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
