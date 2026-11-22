package com.xishen.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.xishen.XishenMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.ChunkPos;
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
 * 基础地形还是原版噪声（一层 NoiseBasedChunkGenerator），但生成完地表之后会做一次"区域后处理"：
 *   - 禁忌之海（区域里）：保持原样——就是海
 *   - 灰雾带：把海水抽干、换成灰黑色的荒原（凝灰岩/黑石/玄武岩），并加一点点起伏
 *
 * 这样"一望无际的海"只出现在禁忌之海区域里，走出去会明显变成干涸荒原，而不是整片维度都是海。
 * 以后加新区域（鬼嘲深渊的峡谷等），就在 {@link #postProcessRegion} 里按区域 id 加分支。
 */
public class GrayRealmChunkGenerator extends ChunkGenerator {
	public static final MapCodec<GrayRealmChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.getBiomeSource()),
			RegistryFixedCodec.create(Registries.NOISE_SETTINGS).fieldOf("settings").forGetter(generator -> generator.settings)
	).apply(instance, GrayRealmChunkGenerator::new));

	/** 荒原地表的几种方块，按位置随机挑，避免一眼看出是同一个方块刷的 */
	private static final BlockState[] ASH_BLOCKS = new BlockState[]{
			Blocks.TUFF.defaultBlockState(),
			Blocks.BLACKSTONE.defaultBlockState(),
			Blocks.BASALT.defaultBlockState(),
			Blocks.GRAVEL.defaultBlockState()
	};

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
		postProcess(chunk, randomState);
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

	/**
	 * 按区域改造地形：
	 *   区域（禁忌之海）→ 不动，保留海水
	 *   灰雾带           → 抽干海水，铺成灰黑荒原（还会垫高 0~2 格做起伏）
	 *   边界灰雾带       → 暂时也不动（以后要做"虚空/深渊"再说）
	 */
	private void postProcess(ChunkAccess chunk, RandomState randomState) {
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
		RandomSource random = randomState.getOrCreateRandomFactory(XishenMod.id("gray_ash")).at(0, 0, 0);

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int x = chunkPos.getMinBlockX() + localX;
				int z = chunkPos.getMinBlockZ() + localZ;
				if (layout.regionIdAt(x, z) != null) {
					continue; // 在区域里（禁忌之海）：保留海水
				}
				if (GrayRealmLayout.distanceFromCenter(x, z) > GrayRealmRegions.WORLD_RADIUS) {
					continue; // 边界灰雾带：暂不处理
				}
				// 灰雾带：先把海平面以下的水换成荒原方块
				for (int y = minY; y <= seaLevel; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (chunk.getBlockState(pos).is(Blocks.WATER)) {
						chunk.setBlockState(pos, ashAt(random, x, y, z), false);
					}
				}
				// 再垫出 0~2 格的小起伏（用海的表面当基准）
				RandomSource columnRandom = randomState.getOrCreateRandomFactory(XishenMod.id("gray_dune")).at(x, 0, z);
				int dune = columnRandom.nextInt(3);
				for (int y = seaLevel + 1; y <= seaLevel + dune; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (chunk.getBlockState(pos).isAir()) {
						chunk.setBlockState(pos, ashAt(random, x, y, z), false);
					}
				}
			}
		}
	}

	private static BlockState ashAt(RandomSource random, int x, int y, int z) {
		int index = Math.floorMod(x * 73856093 ^ y * 19349663 ^ z * 83492791, ASH_BLOCKS.length);
		return ASH_BLOCKS[index];
	}
}
