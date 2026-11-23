package com.xishen.item;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具物品 —— 和原版一样的两套材质：
 *
 * <ul>
 *   <li>灰石工具（对应原版石制工具，Tiers.STONE）：剑 / 镐 / 斧 / 锹 / 锄</li>
 *   <li>血肉工具（对应原版木制工具，Tiers.WOOD）：剑 / 镐 / 斧 / 锹 / 锄</li>
 * </ul>
 *
 * 耐久、挖掘速度、攻击力都直接复用原版的 Tier，所以数值和原版石/木工具完全一致；
 * 合成表也照原版形状（材料换成灰石 / 血肉木板，棍子仍是原版木棍）。
 * 修复材料：灰石工具用灰石（已加进 minecraft:stone_tool_materials），血肉工具用血肉木板（在 minecraft:planks 里）。
 */
public final class XishenItems {
	private XishenItems() {
	}

	private static final List<Item> ALL = new ArrayList<>();

	// ---------------------------------------------------------------- 灰石工具（石级）

	public static final Item GRAY_STONE_SWORD = reg("gray_stone_sword", new SwordItem(Tiers.STONE,
			new Item.Properties().attributes(SwordItem.createAttributes(Tiers.STONE, 3, -2.4F))));

	public static final Item GRAY_STONE_PICKAXE = reg("gray_stone_pickaxe", new PickaxeItem(Tiers.STONE,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.STONE, 1.0F, -2.8F))));

	public static final Item GRAY_STONE_AXE = reg("gray_stone_axe", new AxeItem(Tiers.STONE,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.STONE, 7.0F, -3.2F))));

	public static final Item GRAY_STONE_SHOVEL = reg("gray_stone_shovel", new ShovelItem(Tiers.STONE,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.STONE, 1.5F, -3.0F))));

	public static final Item GRAY_STONE_HOE = reg("gray_stone_hoe", new HoeItem(Tiers.STONE,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.STONE, -1.0F, -2.0F))));

	// ---------------------------------------------------------------- 血肉工具（木级）

	public static final Item FLESH_SWORD = reg("flesh_sword", new SwordItem(Tiers.WOOD,
			new Item.Properties().attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F))));

	public static final Item FLESH_PICKAXE = reg("flesh_pickaxe", new PickaxeItem(Tiers.WOOD,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.WOOD, 1.0F, -2.8F))));

	public static final Item FLESH_AXE = reg("flesh_axe", new AxeItem(Tiers.WOOD,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.WOOD, 7.0F, -3.2F))));

	public static final Item FLESH_SHOVEL = reg("flesh_shovel", new ShovelItem(Tiers.WOOD,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.WOOD, 1.5F, -3.0F))));

	public static final Item FLESH_HOE = reg("flesh_hoe", new HoeItem(Tiers.WOOD,
			new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.WOOD, -1.0F, -2.0F))));

	// ---------------------------------------------------------------- 血肉船

	public static final Item FLESH_BOAT = reg("flesh_boat", new FleshBoatItem(new Item.Properties().stacksTo(1)));

	public static void register() {
		// 战斗页：剑；工具页：镐斧锹锄
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(GRAY_STONE_SWORD);
			entries.accept(FLESH_SWORD);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(GRAY_STONE_PICKAXE);
			entries.accept(GRAY_STONE_AXE);
			entries.accept(GRAY_STONE_SHOVEL);
			entries.accept(GRAY_STONE_HOE);
			entries.accept(FLESH_PICKAXE);
			entries.accept(FLESH_AXE);
			entries.accept(FLESH_SHOVEL);
			entries.accept(FLESH_HOE);
			entries.accept(FLESH_BOAT);
		});
	}

	private static Item reg(String name, Item item) {
		Registry.register(BuiltInRegistries.ITEM, XishenMod.id(name), item);
		ALL.add(item);
		return item;
	}
}
