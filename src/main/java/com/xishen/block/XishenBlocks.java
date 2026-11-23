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
 * 灰界专属方块 —— 全部黑白灰，不带任何颜色（原著：灰界没有颜色）。
 *
 *   xishen:gray_dirt        灰土
 *   xishen:gray_grass_block 灰草方块（顶=灰草、侧=草边、底=灰土）
 *   xishen:gray_stone       灰石（灰界的地层）
 *   xishen:gray_gravel      灰砾石（海床/水下）
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

	/** 灰石 */
	public static final Block GRAY_STONE = new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.5F, 6.0F)
			.sound(SoundType.STONE)
			.requiresCorrectToolForDrops());

	/** 灰砾石 */
	public static final Block GRAY_GRAVEL = new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.6F)
			.sound(SoundType.GRAVEL));


	/** 血肉原木（苦肉浊林的树） */
	public static final Block FLESH_LOG = new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(2.0F)
			.sound(SoundType.WOOD));

	/** 血肉簇（血肉树的"树叶"） */
	public static final Block FLESH_LEAVES = new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(0.3F)
			.sound(SoundType.GRASS));

	public static void register() {
		registerWithItem("gray_dirt", GRAY_DIRT);
		registerWithItem("gray_grass_block", GRAY_GRASS_BLOCK);
		registerWithItem("gray_stone", GRAY_STONE);
		registerWithItem("gray_gravel", GRAY_GRAVEL);
		registerWithItem("flesh_log", FLESH_LOG);
		registerWithItem("flesh_leaves", FLESH_LEAVES);

		// 创造模式物品栏 → 建筑方块
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(GRAY_STONE);
			entries.accept(GRAY_DIRT);
			entries.accept(GRAY_GRASS_BLOCK);
			entries.accept(GRAY_GRAVEL);
			entries.accept(FLESH_LOG);
			entries.accept(FLESH_LEAVES);
		});
	}

	private static void registerWithItem(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, XishenMod.id(name), block);
		Registry.register(BuiltInRegistries.ITEM, XishenMod.id(name), new BlockItem(block, new Item.Properties()));
	}

}
