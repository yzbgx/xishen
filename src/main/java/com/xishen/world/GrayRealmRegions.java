package com.xishen.world;

import java.util.List;

/**
 * 灰界的"区域目录"——只描述有哪些区域、各自什么规格，不描述它们在世界里的具体位置。
 *
 * 具体位置/大小/形状由 GrayRealmLayout 按世界种子生成（每个世界不一样）：
 *   - 位置：不固定，但会挤在灰界地图中间、彼此相邻（一块，不碎开）
 *   - 大小：原书没写死，随机（在下面给的区间里）
 *   - 形状：不规则；原书特别点明的（如鬼嘲深渊是巨大峡谷）以后可以在这里加 shape 参数特判
 *   - 禁忌之海：永远以 (0,0) 为中心（pinnedAtOrigin = true）
 */
public final class GrayRealmRegions {
	private GrayRealmRegions() {
	}

	/**
	 * 一个区域的规格。
	 *
	 * @param id            区域 id，同时也是 dimension JSON 里 regions 表的键
	 * @param pinnedAtOrigin 是否固定在 (0,0) 中心（禁忌之海为 true）
	 * @param radiusMin     半径下限（格）
	 * @param radiusMax     半径上限（格）
	 */
	public record Spec(String id, boolean pinnedAtOrigin, double radiusMin, double radiusMax) {
	}

	/** 目前只有禁忌之海；以后加鬼嘲深渊等，往这里加一条即可 */
	public static final List<Spec> SPECS = List.of(
			new Spec("forbidden_sea", true, 550.0, 800.0));

	/** 灰界这张地图的整体半径：超出去就是边界灰雾带 */
	public static final double WORLD_RADIUS = 3000.0;

	/** 灰雾带里：离最近区域每 800 格，灰蚀等级 +1（最多 +2） */
	public static final double FOG_LEVEL_STEP = 800.0;

	/** 边界灰雾带里：越过地图半径后每 200 格，灰蚀等级 +1（最多 +4） */
	public static final double EDGE_LEVEL_STEP = 200.0;

	/** 非固定区域的摆放环半径范围 */
	public static final double RING_MIN = 1500.0;
	public static final double RING_MAX = 2200.0;

	/** 两个区域之间至少留出多宽的灰雾带（格） */
	public static final double REGION_GAP = 350.0;
}
