package com.xishen.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Map;
import java.util.stream.Stream;

/**
 * 灰界专用 biome source：按 GrayRealmLayout 生成的世界布局分配群系。
 *
 *   - 落在某个区域里   → regions 表里该区域对应的群系（现在只有 forbidden_sea）
 *   - 区域之外、地图内 → 常规灰雾带（区域之间那一片"不规则的空地"）
 *   - 地图之外         → 边界灰雾带
 *
 * 布局每个世界随机（位置/大小/形状），种子从气候采样器里派生 —— 采样器本身由世界种子决定，
 * 所以不同世界布局不同；同一个世界内所有区块用同一个布局。
 *
 * dimension JSON：
 *   "biome_source": {
 *     "type": "xishen:gray_realm",
 *     "regions": { "forbidden_sea": "xishen:forbidden_sea" },
 *     "fog_band": "xishen:gray_fog",
 *     "edge": "xishen:gray_fog_edge"
 *   }
 */
public class GrayRealmBiomeSource extends BiomeSource {
	public static final MapCodec<GrayRealmBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, RegistryFixedCodec.create(Registries.BIOME))
					.fieldOf("regions").forGetter(source -> source.regionBiomes),
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("fog_band").forGetter(source -> source.fogBand),
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("edge").forGetter(source -> source.edge)
	).apply(instance, GrayRealmBiomeSource::new));

	private final Map<String, Holder<Biome>> regionBiomes;
	private final Holder<Biome> fogBand;
	private final Holder<Biome> edge;

	/** 这个世界实际的布局；第一次生成区块时按采样器派生种子创建 */
	private volatile GrayRealmLayout layout;

	public GrayRealmBiomeSource(Map<String, Holder<Biome>> regionBiomes, Holder<Biome> fogBand, Holder<Biome> edge) {
		this.regionBiomes = Map.copyOf(regionBiomes);
		this.fogBand = fogBand;
		this.edge = edge;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return Stream.concat(this.regionBiomes.values().stream(), Stream.of(this.fogBand, this.edge));
	}

	/** 当前世界的布局；还没生成过区块时为 null */
	public GrayRealmLayout layout() {
		return this.layout;
	}

	private GrayRealmLayout layout(Climate.Sampler sampler) {
		GrayRealmLayout current = this.layout;
		if (current == null) {
			synchronized (this) {
				current = this.layout;
				if (current == null) {
					current = GrayRealmLayout.generate(seedFrom(sampler));
					this.layout = current;
				}
			}
		}
		return current;
	}

	/**
	 * 从气候采样器派生一个"世界种子"。
	 * 采样器的密度函数是用世界种子初始化的，所以不同世界取到的值不同、同一世界内恒定。
	 */
	private static long seedFrom(Climate.Sampler sampler) {
		Climate.TargetPoint point = sampler.sample(0, 64, 0);
		return point.temperature() * 0x9E3779B97F4A7C15L
				^ point.humidity() * 0xC2B2AE3D27D4EB4FL
				^ point.continentalness() * 0x165667B19E3779F9L
				^ point.erosion() * 0x27D4EB2F165667C5L
				^ point.depth() * 0x85EBCA77C2B2AE63L
				^ point.weirdness() * 0x9E3779B185EBCA87L;
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		GrayRealmLayout worldLayout = layout(sampler);
		// 参数是"四分坐标"，乘 4 得到方块坐标
		double blockX = quartX << 2;
		double blockZ = quartZ << 2;

		String regionId = worldLayout.regionIdAt(blockX, blockZ);
		if (regionId != null) {
			Holder<Biome> biome = this.regionBiomes.get(regionId);
			if (biome != null) {
				return biome;
			}
		}
		return GrayRealmLayout.distanceFromCenter(blockX, blockZ) <= GrayRealmRegions.WORLD_RADIUS
				? this.fogBand
				: this.edge;
	}
}
