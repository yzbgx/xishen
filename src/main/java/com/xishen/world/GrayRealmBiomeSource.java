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
 * 灰界专用 biome source：按"地图坐标"分配群系。
 *
 *   - 命中 GrayRealmRegions 里某个区域 → 用 regions 表里该区域对应的群系
 *   - 不在任何区域、但在灰界范围内 → 常规灰雾带
 *   - 超出灰界范围（WORLD_RADIUS 外） → 边界灰雾带
 *
 * dimension JSON 里的写法：
 *   "biome_source": {
 *     "type": "xishen:gray_realm",
 *     "regions": { "forbidden_sea": "xishen:forbidden_sea" },
 *     "fog_band": "xishen:gray_fog",
 *     "edge": "xishen:gray_fog_edge"
 *   }
 *
 * 以后加区域：在 GrayRealmRegions.REGIONS 里加地点，再往 regions 表里加 "区域id": "群系id"。
 */
public class GrayRealmBiomeSource extends BiomeSource {
	public static final MapCodec<GrayRealmBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, RegistryFixedCodec.create(Registries.BIOME))
					.fieldOf("regions").forGetter(source -> source.regionBiomes),
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("fog_band").forGetter(source -> source.fogBand),
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("edge").forGetter(source -> source.edge)
	).apply(instance, GrayRealmBiomeSource::new));

	/** 区域 id → 该区域的群系 */
	private final Map<String, Holder<Biome>> regionBiomes;
	private final Holder<Biome> fogBand;
	private final Holder<Biome> edge;

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

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		// 参数是"四分坐标"，乘 4 得到方块坐标
		double blockX = quartX << 2;
		double blockZ = quartZ << 2;

		GrayRealmRegions.Region region = GrayRealmRegions.regionAt(blockX, blockZ);
		if (region != null) {
			Holder<Biome> biome = this.regionBiomes.get(region.id());
			if (biome != null) {
				return biome;
			}
		}
		return GrayRealmRegions.distanceFromCenter(blockX, blockZ) <= GrayRealmRegions.WORLD_RADIUS
				? this.fogBand
				: this.edge;
	}
}
