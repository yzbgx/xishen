package com.xishen.block;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 灰界专属方块 —— 全部黑白灰，不带任何颜色（原著：灰界没有颜色）。
 *
 * <p>基础方块：
 * <ul>
 *   <li>xishen:gray_dirt        灰土</li>
 *   <li>xishen:gray_grass_block 灰草方块</li>
 *   <li>xishen:gray_stone       灰石（灰界地层）</li>
 *   <li>xishen:gray_gravel      灰砾石（海床）</li>
 *   <li>xishen:flesh_log        血肉原木（苦肉浊林的树）</li>
 *   <li>xishen:flesh_leaves     血肉簇</li>
 * </ul>
 *
 * <p>按原版"方块家族"逻辑延伸出来的方块（合成表与原版同规则，见 data/xishen/recipe）：
 * <ul>
 *   <li>灰石家族：灰石台阶 / 楼梯 / 墙 / 压力板 / 按钮；灰石砖 + 灰石砖台阶 / 楼梯 / 墙</li>
 *   <li>血肉家族：血肉木板 / 台阶 / 楼梯 / 栅栏 / 栅栏门 / 门 / 活板门 / 按钮 / 压力板</li>
 *   <li>灰土家族：粗灰土（灰土 + 灰砾石）</li>
 * </ul>
 */
public final class XishenBlocks {
	private XishenBlocks() {
	}

	/** 所有方块（注册顺序即创造栏顺序） */
	private static final List<Block> ALL = new ArrayList<>();

	// ---------------------------------------------------------------- 基础方块

	/** 灰土 */
	public static final Block GRAY_DIRT = reg("gray_dirt", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.5F)
			.sound(SoundType.GRAVEL)));

	/** 粗灰土（灰土 + 灰砾石合成，和原版"粗土"对应） */
	public static final Block GRAY_COARSE_DIRT = reg("gray_coarse_dirt", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.5F)
			.sound(SoundType.GRAVEL)));

	/** 灰草方块 */
	public static final Block GRAY_GRASS_BLOCK = reg("gray_grass_block", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.6F)
			.sound(SoundType.GRASS)));

	/** 灰石 */
	public static final Block GRAY_STONE = reg("gray_stone", new Block(stoneProps().strength(1.5F, 6.0F)));

	/** 灰砾石 */
	public static final Block GRAY_GRAVEL = reg("gray_gravel", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(0.6F)
			.sound(SoundType.GRAVEL)));

	/** 血肉原木 */
	public static final Block FLESH_LOG = reg("flesh_log", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(2.0F)
			.sound(SoundType.WOOD)));

	/** 血肉簇 */
	public static final Block FLESH_LEAVES = reg("flesh_leaves", new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(0.3F)
			.sound(SoundType.GRASS)));

	// ---------------------------------------------------------------- 灰石家族

	public static final Block GRAY_STONE_SLAB = reg("gray_stone_slab",
			new SlabBlock(stoneProps().strength(1.5F, 6.0F)));

	public static final Block GRAY_STONE_STAIRS = reg("gray_stone_stairs",
			new StairBlock(GRAY_STONE.defaultBlockState(), stoneProps().strength(1.5F, 6.0F)));

	public static final Block GRAY_STONE_WALL = reg("gray_stone_wall",
			new WallBlock(stoneProps().strength(1.5F, 6.0F)));

	public static final Block GRAY_STONE_PRESSURE_PLATE = reg("gray_stone_pressure_plate",
			new PressurePlateBlock(BlockSetType.STONE, stoneProps().strength(0.5F)
					.noCollission()));

	public static final Block GRAY_STONE_BUTTON = reg("gray_stone_button",
			new ButtonBlock(BlockSetType.STONE, 20, stoneProps().strength(0.5F).noCollission()));

	/** 灰石砖 */
	public static final Block GRAY_STONE_BRICKS = reg("gray_stone_bricks", new Block(stoneProps().strength(1.5F, 6.0F)));

	public static final Block GRAY_STONE_BRICKS_SLAB = reg("gray_stone_bricks_slab",
			new SlabBlock(stoneProps().strength(1.5F, 6.0F)));

	public static final Block GRAY_STONE_BRICKS_STAIRS = reg("gray_stone_bricks_stairs",
			new StairBlock(GRAY_STONE_BRICKS.defaultBlockState(), stoneProps().strength(1.5F, 6.0F)));

	public static final Block GRAY_STONE_BRICKS_WALL = reg("gray_stone_bricks_wall",
			new WallBlock(stoneProps().strength(1.5F, 6.0F)));

	// ---------------------------------------------------------------- 血肉家族（原木 → 木板 → 一套木制品）

	/** 血肉木板 */
	public static final Block FLESH_PLANKS = reg("flesh_planks", new Block(woodProps().strength(2.0F, 3.0F)));

	public static final Block FLESH_STAIRS = reg("flesh_stairs",
			new StairBlock(FLESH_PLANKS.defaultBlockState(), woodProps().strength(2.0F, 3.0F)));

	public static final Block FLESH_SLAB = reg("flesh_slab", new SlabBlock(woodProps().strength(2.0F, 3.0F)));

	public static final Block FLESH_FENCE = reg("flesh_fence", new FenceBlock(woodProps().strength(2.0F, 3.0F)));

	public static final Block FLESH_FENCE_GATE = reg("flesh_fence_gate",
			new FenceGateBlock(WoodType.OAK, woodProps().strength(2.0F, 3.0F)));

	public static final Block FLESH_DOOR = reg("flesh_door", new DoorBlock(BlockSetType.OAK,
			woodProps().strength(3.0F).noOcclusion()));

	public static final Block FLESH_TRAPDOOR = reg("flesh_trapdoor", new TrapDoorBlock(BlockSetType.OAK,
			woodProps().strength(3.0F).noOcclusion()));

	public static final Block FLESH_BUTTON = reg("flesh_button",
			new ButtonBlock(BlockSetType.OAK, 30, woodProps().strength(0.5F).noCollission()));

	public static final Block FLESH_PRESSURE_PLATE = reg("flesh_pressure_plate",
			new PressurePlateBlock(BlockSetType.OAK, woodProps().strength(0.5F).noCollission()));

	// ---------------------------------------------------------------- 属性工具

	private static BlockBehaviour.Properties stoneProps() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.sound(SoundType.STONE)
				.requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties woodProps() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_RED)
				.sound(SoundType.WOOD);
	}

	// ---------------------------------------------------------------- 注册

	public static void register() {
		// 创造模式物品栏 → 建筑方块（ALL 里已经按定义顺序排好）
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			for (Block block : ALL) {
				entries.accept(block);
			}
		});
	}

	private static Block reg(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, XishenMod.id(name), block);
		BlockItem item = new BlockItem(block, new Item.Properties());
		Registry.register(BuiltInRegistries.ITEM, XishenMod.id(name), item);
		ALL.add(block);
		return block;
	}
}
