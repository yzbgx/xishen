package com.xishen.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * 灰界专用 biome source：按坐标把维度切成三圈。
 *
 * 区域位置固定（GrayRealmRegions 里的圆心+半径），所以群系也是固定几何：
 *   半径内      → 区域群系（现在是 禁忌之海 xishen:forbidden_sea）
 *   半径 ~ +200 → 常规灰雾带 xishen:gray_fog
 *   再往外      → 边界灰雾带 xishen:gray_fog_edge（更浓的雾，伤害更高）
 *
 * 在 dimension JSON 里这样引用：
 *   "biome_source": {
 *     "type": "xishen:gray_realm",
 *     "region": "xishen:forbidden_sea",
 *     "fog_band": "xishen:gray_fog",
 *     "edge": "xishen:gray_fog_edge"
 *   }
 *
 * 以后加新区域时，改 GrayRealmRegions.REGIONS 的比例逻辑即可（多区域再扩展这里的选择逻辑）。
 */
public class GrayRealmBiomeSource extends BiomeSource {
	public static final MapCodec<GrayRealmBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("region").forGetter(source -> source.region),
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("fog_band").forGetter(source -> source.fogBand),
			RegistryFixedCodec.create(Registries.BIOME).fieldOf("edge").forGetter(source -> source.edge)
	).apply(instance, GrayRealmBiomeSource::new));

	private final Holder<Biome> region;
	private final Holder<Biome> fogBand;
	private final Holder<Biome> edge;

	public GrayRealmBiomeSource(Holder<Biome> region, Holder<Biome> fogBand, Holder<Biome> edge) {
		this.region = region;
		this.fogBand = fogBand;
		this.edge = edge;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return Stream.of(this.region, this.fogBand, this.edge);
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		// 参数是"四分坐标"，乘 4 得到方块坐标
		double blockX = quartX << 2;
		double blockZ = quartZ << 2;
		double distance = GrayRealmRegions.distanceOutside(blockX, blockZ);
		if (distance <= 0.0) {
			return this.region;
		}
		if (distance <= GrayRealmRegions.FOG_WIDTH) {
			return this.fogBand;
		}
		return this.edge;
	}
}
