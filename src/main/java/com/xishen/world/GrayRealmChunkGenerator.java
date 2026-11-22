package com.xishen.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.xishen.block.XishenBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 灰界专用地形生成器：地形按"区域"分别处理。
 *
 * 基础地形还是原版噪声（内部委托 NoiseBasedChunkGenerator），生成完地表后再做一次区域后处理：
 *   - 禁忌之海（区域里）：保持原样——就是海
 *   - 灰雾带：抽干海水，铺成灰界专属地表——最上面一层灰草方块，下面 3 层灰土，再往下是黑石；
 *             并用低频噪声做 0~5 格的平缓起伏（不做高山，大山留给"虚妄山脉"）
 *   - 边界灰雾带：暂时不处理
 *
 * 以后加新区域（鬼嘲深渊的峡谷等），就在 {@link #postProcess} 里按区域 id 加分支。
 */
public class GrayRealmChunkGenerator extends ChunkGenerator {
	public static final MapCodec<GrayRealmChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.getBiomeSource()),
			RegistryFixedCodec.create(Registries.NOISE_SETTINGS).fieldOf("settings").forGetter(generator -> generator.settings)
	).apply(instance, GrayRealmChunkGenerator::new));

	private final Holder<NoiseGeneratorSettings> settings;
	private final NoiseBasedChunkGenerator delegate;

	public GrayRealmChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
		super(biomeSource);
		this.settings = settings;
		this.delegate = new NoiseBasedChunkGenerator(biomeSource, settings);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	// ---------------------------------------------------------------- 委托给原版生成器

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
		return this.delegate.fillFromNoise(blender, randomState, structureManager, chunk);
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
		this.delegate.buildSurface(region, structureManager, randomState, chunk);
		postProcess(chunk);
	}

	@Override
	public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving) {
		this.delegate.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk, carving);
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
		this.delegate.spawnOriginalMobs(region);
	}

	@Override
	public int getGenDepth() {
		return this.delegate.getGenDepth();
	}

	@Override
	public int getSeaLevel() {
		return this.delegate.getSeaLevel();
	}

	@Override
	public int getMinY() {
		return this.delegate.getMinY();
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
		return this.delegate.getBaseHeight(x, z, type, level, randomState);
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
		return this.delegate.getBaseColumn(x, z, level, randomState);
	}

	@Override
	public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
		this.delegate.addDebugScreenInfo(lines, randomState, pos);
	}

	// ---------------------------------------------------------------- 区域后处理

	private void postProcess(ChunkAccess chunk) {
		if (!(getBiomeSource() instanceof GrayRealmBiomeSource biomeSource)) {
			return;
		}
		GrayRealmLayout layout = biomeSource.layout();
		if (layout == null) {
			return;
		}
		ChunkPos chunkPos = chunk.getPos();
		int seaLevel = getSeaLevel();
		int minY = chunk.getMinBuildHeight();
		BlockState ash = Blocks.BLACKSTONE.defaultBlockState();
		BlockState grass = XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState();
		BlockState dirt = XishenBlocks.GRAY_DIRT.defaultBlockState();

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int x = chunkPos.getMinBlockX() + localX;
				int z = chunkPos.getMinBlockZ() + localZ;
				if (layout.regionIdAt(x, z) != null) {
					continue; // 禁忌之海：保留海水
				}
				if (GrayRealmLayout.distanceFromCenter(x, z) > GrayRealmRegions.WORLD_RADIUS) {
					continue; // 边界灰雾带：暂不处理
				}
				int top = seaLevel + hillHeight(x, z);

				// 抽干：海平面以下的水、以及垫高出来的空气，全部换成黑石
				for (int y = minY; y <= top; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = chunk.getBlockState(pos);
					if (state.is(Blocks.WATER) || (y > seaLevel && state.isAir())) {
						chunk.setBlockState(pos, ash, false);
					}
				}
				// 表层：灰草 + 3 层灰土
				chunk.setBlockState(new BlockPos(x, top, z), grass, false);
				for (int depth = 1; depth <= 3; depth++) {
					chunk.setBlockState(new BlockPos(x, top - depth, z), dirt, false);
				}
			}
		}
	}

	/** 平缓起伏：0~5 格，用两层值噪声叠加（尺度 ~48 格和 ~24 格） */
	private static int hillHeight(int x, int z) {
		double n = valueNoise(x / 48.0, z / 48.0, 0x5EED_0001L) * 0.7
				+ valueNoise(x / 24.0, z / 24.0, 0x5EED_0002L) * 0.3;
		int h = (int) Math.round((n + 1.0) * 2.5);
		return Math.max(0, Math.min(5, h));
	}

	private static double valueNoise(double x, double z, long seed) {
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
