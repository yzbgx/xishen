package com.xishen.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 一个世界里灰界区域的"实际布局"：谁在哪、多大、边界长什么样。
 *
 * 生成规则（按你的设定）：
 *   - 每个世界随机，但**单个区域必须是一整块**（不会碎成几片），区域之间**留出空隙**（空隙就是灰雾带）；
 *   - 禁忌之海永远在 (0,0)；
 *   - 半径在 GrayRealmRegions.SPECS 给的区间里随机；
 *   - 形状不规则：半径乘以一个低频噪声，让边界变成起伏的曲线而不是正圆；
 *     以后原书特别点明的形状（鬼嘲深渊=巨大峡谷）可以在这里按 id 特判拉伸。
 *
 * 单块保证：判定用"到区域中心、被噪声扰动后的归一化距离 < 1"，所以每个区域就是一个连通的
 * 变形圆盘；不在任何区域里的点一律算灰雾带，所以区域之间天然有雾的空隙。
 *
 * 这个对象是不可变的，可以安全地在多个线程/多次调用间复用。
 */
public final class GrayRealmLayout {
	/** 布局里的一块区域 */
	public record PlacedRegion(String id, double centerX, double centerZ, double radius) {
	}

	private final List<PlacedRegion> regions;
	private final long noiseSeed;

	private GrayRealmLayout(List<PlacedRegion> regions, long noiseSeed) {
		this.regions = List.copyOf(regions);
		this.noiseSeed = noiseSeed;
	}

	public List<PlacedRegion> regions() {
		return this.regions;
	}

	/** 按世界种子生成布局 */
	public static GrayRealmLayout generate(long seed) {
		Random random = new Random(seed);
		List<PlacedRegion> placed = new ArrayList<>();

		List<GrayRealmRegions.Spec> specs = GrayRealmRegions.SPECS;
		int freeCount = 0;
		for (GrayRealmRegions.Spec spec : specs) {
			if (!spec.pinnedAtOrigin()) {
				freeCount++;
			}
		}

		int freeIndex = 0;
		for (GrayRealmRegions.Spec spec : specs) {
			double radius = spec.radiusMin() + random.nextDouble() * (spec.radiusMax() - spec.radiusMin());
			double cx;
			double cz;
			if (spec.pinnedAtOrigin()) {
				// 禁忌之海：永远在原点
				cx = 0.0;
				cz = 0.0;
			} else {
				// 其它区域：环状随机摆放；和已摆好的区域保持间隔，保证两块区域之间留出灰雾带
				double angle = (Math.PI * 2.0) * freeIndex / Math.max(1, freeCount)
						+ (random.nextDouble() - 0.5) * 0.6;
				double distance = GrayRealmRegions.RING_MIN
						+ random.nextDouble() * (GrayRealmRegions.RING_MAX - GrayRealmRegions.RING_MIN);
				cx = Math.cos(angle) * distance;
				cz = Math.sin(angle) * distance;
				// 太挤就往远处挪：确保和已有区域之间至少留 GAP 的空隙
				for (int attempt = 0; attempt < 12; attempt++) {
					boolean tooClose = false;
					for (PlacedRegion other : placed) {
						double minDistance = radius + other.radius() + GrayRealmRegions.REGION_GAP;
						if (Math.hypot(cx - other.centerX(), cz - other.centerZ()) < minDistance) {
							tooClose = true;
							break;
						}
					}
					if (!tooClose) {
						break;
					}
					cx *= 1.15;
					cz *= 1.15;
				}
				freeIndex++;
			}
			placed.add(new PlacedRegion(spec.id(), cx, cz, radius));
		}

		return new GrayRealmLayout(placed, seed * 0x9E3779B97F4A7C15L + 0x1234_5678_9ABC_DEF0L);
	}

	/** 按 id 找布局里的区域 */
	public PlacedRegion regionById(String id) {
		for (PlacedRegion region : this.regions) {
			if (region.id().equals(id)) {
				return region;
			}
		}
		return null;
	}

	/** 该点属于哪个区域（按"半径噪声扰动后的距离"归一化后取最近）；不属于任何区域返回 null */
	public String regionIdAt(double x, double z) {
		String best = null;
		double bestScore = 1.0;
		for (PlacedRegion region : this.regions) {
			double score = normalizedDistance(region, x, z);
			if (score < bestScore) {
				bestScore = score;
				best = region.id();
			}
		}
		return best;
	}

	/** 归一化距离：< 1 表示在区域里；噪声让边界不规则 */
	private double normalizedDistance(PlacedRegion region, double x, double z) {
		double dx = x - region.centerX();
		double dz = z - region.centerZ();
		double distance = Math.hypot(dx, dz);
		// 用低频噪声扰动半径：边界变成起伏的曲线（±25%）
		double wobble = 1.0 + 0.25 * noise(this.noiseSeed, region.id().hashCode(), x * 0.0018, z * 0.0018);
		return distance / (region.radius() * wobble);
	}

	/** 到最近区域边界的大致距离（在区域里为 0） */
	public double distanceToNearestRegion(double x, double z) {
		double best = Double.MAX_VALUE;
		for (PlacedRegion region : this.regions) {
			double effectiveRadius = region.radius() * (1.0 + 0.25 * noise(this.noiseSeed, region.id().hashCode(), x * 0.0018, z * 0.0018));
			double d = Math.max(0.0, Math.hypot(x - region.centerX(), z - region.centerZ()) - effectiveRadius);
			best = Math.min(best, d);
		}
		return best;
	}

	public static double distanceFromCenter(double x, double z) {
		return Math.hypot(x, z);
	}

	/** 灰蚀等级：区域 0；灰雾带 1~3；边界 4~8（参考虚空） */
	public int erosionLevel(double x, double z) {
		if (regionIdAt(x, z) != null) {
			return 0;
		}
		double fromCenter = distanceFromCenter(x, z);
		if (fromCenter <= GrayRealmRegions.WORLD_RADIUS) {
			return 1 + (int) Math.min(2, distanceToNearestRegion(x, z) / GrayRealmRegions.FOG_LEVEL_STEP);
		}
		double beyond = fromCenter - GrayRealmRegions.WORLD_RADIUS;
		return 4 + (int) Math.min(4, beyond / GrayRealmRegions.EDGE_LEVEL_STEP);
	}

	// ---------------------------------------------------------------- 噪声

	/** 二维值噪声（多倍频叠加），范围约 [-1, 1] */
	private static double noise(long seed, int idHash, double x, double z) {
		double value = 0.0;
		double amplitude = 1.0;
		double frequency = 1.0;
		double total = 0.0;
		for (int octave = 0; octave < 3; octave++) {
			value += amplitude * valueNoise(seed + idHash * 31L + octave * 7919L, x * frequency, z * frequency);
			total += amplitude;
			amplitude *= 0.5;
			frequency *= 2.0;
		}
		return value / total;
	}

	private static double valueNoise(long seed, double x, double z) {
		int x0 = (int) Math.floor(x);
		int z0 = (int) Math.floor(z);
		double fx = smooth(x - x0);
		double fz = smooth(z - z0);
		double v00 = hash(seed, x0, z0);
		double v10 = hash(seed, x0 + 1, z0);
		double v01 = hash(seed, x0, z0 + 1);
		double v11 = hash(seed, x0 + 1, z0 + 1);
		double top = v00 + (v10 - v00) * fx;
		double bottom = v01 + (v11 - v01) * fx;
		return top + (bottom - top) * fz;
	}

	private static double smooth(double t) {
		return t * t * (3.0 - 2.0 * t);
	}

	private static double hash(long seed, int x, int z) {
		long h = seed;
		h = h * 6364136223846793005L + 1442695040888963407L + x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return ((h >>> 11) & 0x1FFFFFFFFFFFFFL) / (double) 0x1FFFFFFFFFFFFFL * 2.0 - 1.0;
	}
}
