package com.xishen.world;

import java.util.List;

/**
 * 灰界的区域布局。
 *
 * 按你的设定：区域位置固定，区域内地形随机。这里只用"圆心 + 半径"来定义每块区域占据的位置，
 * 以后加「鬼嘲深渊 / 苦肉浊林 / 叹息旷野 / 虚妄山脉 / 风暴孤岛」时，往 REGIONS 里加一条就行。
 *
 * 三种地带（由内到外）：
 *   SOLID  实体区域：正常地形，灰蚀 0 级
 *   FOG    灰雾带  ：区域外缘 FOG_WIDTH 格宽的一圈灰雾，失明 + 灰蚀 1 级
 *   EDGE   边缘    ：再往外，雾更浓（黑暗效果）+ 灰蚀等级随距离递增，参考虚空
 */
public final class GrayRealmRegions {
	private GrayRealmRegions() {
	}

	/** 一块区域：id、中心 x/z、半径（格） */
	public record Region(String id, double centerX, double centerZ, double radius) {
	}

	/** 目前只有禁忌之海，中心 (0,0)，半径 600 格 */
	public static final List<Region> REGIONS = List.of(
			new Region("forbidden_sea", 0.0, 0.0, 600.0));

	/** 灰雾带宽度（格） */
	public static final double FOG_WIDTH = 200.0;
	/** 边缘区：每往外 150 格，灰蚀等级 +1 */
	public static final double EDGE_STEP = 150.0;
	/** 边缘区灰蚀等级上限（2 + EDGE_MAX_EXTRA） */
	public static final int EDGE_MAX_EXTRA = 6;

	public enum Zone {
		SOLID, FOG, EDGE
	}

	/** 到最近区域边缘的距离，负数表示在区域内 */
	public static double distanceOutside(double x, double z) {
		double best = Double.MAX_VALUE;
		for (Region region : REGIONS) {
			double d = Math.hypot(x - region.centerX(), z - region.centerZ()) - region.radius();
			if (d < best) {
				best = d;
			}
		}
		return best;
	}

	public static Zone zoneAt(double x, double z) {
		double d = distanceOutside(x, z);
		if (d <= 0) {
			return Zone.SOLID;
		}
		if (d <= FOG_WIDTH) {
			return Zone.FOG;
		}
		return Zone.EDGE;
	}

	/**
	 * 灰蚀等级：
	 * 区域内 0 级；灰雾带 1 级；边缘从 2 级起，越远越高（最高 8 级）。
	 */
	public static int erosionLevel(double x, double z) {
		double d = distanceOutside(x, z);
		if (d <= 0) {
			return 0;
		}
		if (d <= FOG_WIDTH) {
			return 1;
		}
		int extra = (int) Math.min(EDGE_MAX_EXTRA, (d - FOG_WIDTH) / EDGE_STEP);
		return 2 + extra;
	}
}
