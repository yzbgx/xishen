package com.xishen.block;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 灰界专属方块。
 *
 * 先是两种地表方块：
 *   xishen:gray_dirt        灰土
 *   xishen:gray_grass_block 灰草方块（顶=灰草、侧=草边、底=灰土）
 *
 * 贴图在 assets/xishen/textures/block/ 下，模型/方块状态/战利品表也都在资源里，
 * 这里只负责注册。
 */
public final class XishenBlocks {
	private XishenBlocks() {
	}

	/** 灰土 */
	public static final Block GRAY_DIRT = new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.5F)
			.sound(SoundType.GRAVEL));

	/** 灰草方块 */
	public static final Block GRAY_GRASS_BLOCK = new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.6F)
			.sound(SoundType.GRASS));

	public static void register() {
		registerWithItem("gray_dirt", GRAY_DIRT);
		registerWithItem("gray_grass_block", GRAY_GRASS_BLOCK);

		// 放进创造模式物品栏的"建筑方块"页
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(GRAY_DIRT);
			entries.accept(GRAY_GRASS_BLOCK);
		});
	}

	private static void registerWithItem(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, XishenMod.id(name), block);
		Registry.register(BuiltInRegistries.ITEM, XishenMod.id(name), new BlockItem(block, new Item.Properties()));
	}
}
