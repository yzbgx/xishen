package com.xishen.world;

import java.util.List;

/**
 * 灰界的区域布局 —— 按原著：灰界是一张"地图"，各区是**地图上不同位置的若干片地方**，
 * 不是一圈套一圈的同心圆。
 *
 * 所以这里用"每个区域一个地点（圆心 + 半径）"来表示，区域之间自然留出空隙：
 *
 *   区域（Region）    地图上的一个个地点，各自一个群系（现在只有 禁忌之海，在 (0,0)）
 *   灰雾带（Fog）     区域之外、灰界范围之内的所有地方 —— 也就是"区域与区域之间的连接地带"
 *   边界灰雾带（Edge） 离灰界中心超过 WORLD_RADIUS 的地方 —— 灰界的外圈边界（雾最浓、伤害最高）
 *
 * 以后加「鬼嘲深渊 / 苦肉浊林 / 叹息旷野 / 虚妄山脉 / 风暴孤岛」时，往 REGIONS 里加一条
 * （各自的坐标、半径、id），它们之间的空地就自动变成灰雾带，不需要改别的逻辑。
 */
public final class GrayRealmRegions {
	private GrayRealmRegions() {
	}

	/** 一块区域：id、中心 x/z、半径（格） */
	public record Region(String id, double centerX, double centerZ, double radius) {
		public boolean contains(double x, double z) {
			return Math.hypot(x - centerX, z - centerZ) <= radius;
		}

		/** 到区域边缘的距离（区域内为 0） */
		public double distanceFrom(double x, double z) {
			return Math.max(0.0, Math.hypot(x - centerX, z - centerZ) - radius);
		}
	}

	/** 目前只有禁忌之海；以后按原著再加新地点即可 */
	public static final List<Region> REGIONS = List.of(
			new Region("forbidden_sea", 0.0, 0.0, 600.0));

	/** 灰界的"地图半径"：超过这个距离就是边界灰雾带（灰界的外圈） */
	public static final double WORLD_RADIUS = 3000.0;

	/** 灰雾带里：离最近区域每 800 格，灰蚀等级 +1（最多 +2） */
	public static final double FOG_LEVEL_STEP = 800.0;
	/** 边界灰雾带里：越过地图半径后每 200 格，灰蚀等级 +1（最多 +4） */
	public static final double EDGE_LEVEL_STEP = 200.0;

	public enum Zone {
		/** 在某个区域里 */
		REGION,
		/** 区域之间 / 区域周围，灰界范围内 */
		FOG,
		/** 灰界外圈 */
		EDGE
	}

	/** 该点所在的区域；不在任何区域里返回 null */
	public static Region regionAt(double x, double z) {
		for (Region region : REGIONS) {
			if (region.contains(x, z)) {
				return region;
			}
		}
		return null;
	}

	/** 到最近区域边缘的距离（在区域里为 0） */
	public static double distanceToNearestRegion(double x, double z) {
		double best = Double.MAX_VALUE;
		for (Region region : REGIONS) {
			best = Math.min(best, region.distanceFrom(x, z));
		}
		return best;
	}

	/** 到灰界地图中心的距离 */
	public static double distanceFromCenter(double x, double z) {
		return Math.hypot(x, z);
	}

	public static Zone zoneAt(double x, double z) {
		if (regionAt(x, z) != null) {
			return Zone.REGION;
		}
		return distanceFromCenter(x, z) <= WORLD_RADIUS ? Zone.FOG : Zone.EDGE;
	}

	/**
	 * 灰蚀等级：
	 *   区域里     0 级（1 点/2 秒）
	 *   灰雾带     1 级起，离区域越远越高，最高 3 级
	 *   边界灰雾带 4 级起，越往外越高，最高 8 级（参考虚空）
	 */
	public static int erosionLevel(double x, double z) {
		Region region = regionAt(x, z);
		if (region != null) {
			return 0;
		}
		double toRegion = distanceToNearestRegion(x, z);
		if (distanceFromCenter(x, z) <= WORLD_RADIUS) {
			return 1 + (int) Math.min(2, toRegion / FOG_LEVEL_STEP);
		}
		double beyond = distanceFromCenter(x, z) - WORLD_RADIUS;
		return 4 + (int) Math.min(4, beyond / EDGE_LEVEL_STEP);
	}
}
