package com.xishen.path;

/**
 * 神道熟练度的所有数值集中在这里，调平衡只改这个文件。
 */
public final class PathValues {
	private PathValues() {
	}

	// ---------------- 熟练度获取（每次行为的增量）

	/** 挖任意方块 → 力 */
	public static final int LI_PER_BLOCK = 1;
	/** 收割作物 → 黄 */
	public static final int HUANG_PER_CROP = 1;
	/** 与村民交易一次 → 黄 */
	public static final int HUANG_PER_TRADE = 2;
	/** 普通击杀 → 兵 */
	public static final int BING_PER_KILL = 3;
	/** 潜行击杀 → 娼（刺客） */
	public static final int CHANG_PER_SNEAK_KILL = 5;
	/** 取走合成成品 → 巧 */
	public static final int QIAO_PER_CRAFT = 2;
	/** 放置方块 → 巧 */
	public static final int QIAO_PER_PLACE = 1;
	/** 放置红石元件 → 弈 */
	public static final int YIX_PER_REDSTONE = 1;
	/** 使用末影之眼 → 卜 */
	public static final int BU_PER_EYE = 3;
	/** 进入新群系 → 卜 */
	public static final int BU_PER_BIOME = 2;
	/** 放置画作 → 青 */
	public static final int QING_PER_PAINTING = 3;
	/** 使用附魔台 → 书 */
	public static final int SHU_PER_ENCHANT = 2;
	/** 使用酿造台 → 巫 */
	public static final int WU_PER_BREW = 2;
	/** 开箱子/木桶 → 盗 */
	public static final int DAO_PER_CHEST = 1;
	/** 驯服动物 → 偶 */
	public static final int OU_PER_TAME = 2;
	/** 吃东西 → 医 */
	public static final int YI_PER_EAT = 2;
	/** 音符盒/唱片机 → 戏 */
	public static final int XI_PER_MUSIC = 2;

	// ---------------- 加点

	/** 1 点加点换多少熟练度 */
	public static final int PROFICIENCY_PER_POINT = 10;
}
