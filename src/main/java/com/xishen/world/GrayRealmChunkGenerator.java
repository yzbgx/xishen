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
		BlockState fill = XishenBlocks.GRAY_STONE.defaultBlockState();
		BlockState grass = XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState();
		BlockState dirt = XishenBlocks.GRAY_DIRT.defaultBlockState();

		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int x = chunkPos.getMinBlockX() + localX;
				int z = chunkPos.getMinBlockZ() + localZ;
				String regionId = layout.regionIdAt(x, z);
				if (regionId != null) {
					// 按区域 id 选地形（禁忌之海保持海水，什么都不做）
					if ("forbidden_sea".equals(regionId)) {
						buildForbiddenSea(chunk, x, z, minY, seaLevel);
					} else if ("ghost_abyss".equals(regionId)) {
						carveAbyss(chunk, layout, x, z, minY, seaLevel);
					} else if ("rot_forest".equals(regionId)) {
						buildRotForest(chunk, x, z, minY, seaLevel);
					} else if ("sigh_plain".equals(regionId)) {
						buildSighPlain(chunk, x, z, minY, seaLevel);
					} else if ("vain_range".equals(regionId)) {
						buildVainRange(chunk, x, z, minY, seaLevel);
					}
					continue;
				}

				int top = seaLevel + hillHeight(x, z);

				// 抽干：海平面以下的水、以及垫高出来的空气，全部换成黑石
				for (int y = minY; y <= top; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = chunk.getBlockState(pos);
					if (state.is(Blocks.WATER) || (y > seaLevel && state.isAir())) {
						chunk.setBlockState(pos, fill, false);
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

	/**
	 * 禁忌之海：保留海水，但给海"垫底"。
	 *
	 * 原版噪声地形在这个维度里最低能到 y=0 以下，水会一路深到岩浆层（一百多格），
	 * 底部水和岩浆交界还会生成黑曜石——所以这里把海床固定到海平面下 49~61 格：
	 * 海床以下填灰石、海床铺灰砾石，海底到海平面之间若出现岩浆/黑曜石也一并填掉。
	 */
	private void buildForbiddenSea(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		int depth = 55 + (int) Math.round(valueNoise(x / 90.0, z / 90.0, 0x5EA1L) * 6.0);
		int floor = seaLevel - depth;
		BlockState stone = XishenBlocks.GRAY_STONE.defaultBlockState();
		BlockState gravel = XishenBlocks.GRAY_GRAVEL.defaultBlockState();

		for (int y = minY; y <= floor; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = chunk.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) {
				chunk.setBlockState(pos, stone, false);
			}
		}
		chunk.setBlockState(new BlockPos(x, floor, z), gravel, false);

		for (int y = floor + 1; y <= seaLevel; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = chunk.getBlockState(pos);
			if (state.is(Blocks.LAVA) || state.is(Blocks.OBSIDIAN) || state.is(Blocks.MAGMA_BLOCK)) {
				chunk.setBlockState(pos, stone, false);
			}
		}
	}

	/**
	 * 鬼嘲深渊：在区域里挖一个"碗形深渊"。
	 * 越靠近区域中心越深（最深约 70 格，中心底部 y≈26），按 3 格一台阶做成阶梯状陡壁；
	 * 底部铺灰土、墙壁和内部是灰石，上方抽成空气（没有水，也没有岛）。
	 */
	private void carveAbyss(ChunkAccess chunk, GrayRealmLayout layout, int x, int z, int minY, int seaLevel) {
		GrayRealmLayout.PlacedRegion abyss = layout.regionById("ghost_abyss");
		if (abyss == null) {
			return;
		}
		BlockState stone = XishenBlocks.GRAY_STONE.defaultBlockState();
		BlockState dirt = XishenBlocks.GRAY_DIRT.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		double distance = Math.hypot(x - abyss.centerX(), z - abyss.centerZ());
		double ratio = Math.min(1.0, distance / abyss.radius());
		int depth = (int) Math.round(Math.pow(1.0 - ratio, 1.25) * 70.0);
		int floor = seaLevel - (depth / 3) * 3;   // 3 格一个台阶

		// 坑底以下：只补 8 格（再往下本来就是实心，没必要整列重写，省性能）
		for (int y = Math.max(minY, floor - 8); y <= floor; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState current = chunk.getBlockState(pos);
			if (current.isAir() || !current.getFluidState().isEmpty()) {
				chunk.setBlockState(pos, stone, false);
			}
		}
		// 坑底以上到海平面：抽空（水、土、石头全部挖掉，形成陡壁）
		for (int y = floor + 1; y <= seaLevel; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!chunk.getBlockState(pos).isAir()) {
				chunk.setBlockState(pos, air, false);
			}
		}
		// 坑底地表：灰土
		chunk.setBlockState(new BlockPos(x, floor, z), dirt, false);
		if (floor - 1 >= minY) {
			chunk.setBlockState(new BlockPos(x, floor - 1, z), stone, false);
		}
	}
	/**
	 * 通用塑形：先把 top 以上的水抽干、把山体填实，再铺顶层方块。
	 * 灰雾带之外的新区域（苦肉浊林 / 叹息旷野 / 虚妄山脉）都用它。
	 */
	private void shapeColumn(ChunkAccess chunk, int x, int z, int minY, int seaLevel, int top,
			BlockState topBlock, BlockState filler) {
		BlockState air = Blocks.AIR.defaultBlockState();
		// 1) 地表以上的残留水：抽干
		for (int y = top + 1; y <= seaLevel; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (chunk.getBlockState(pos).is(Blocks.WATER)) {
				chunk.setBlockState(pos, air, false);
			}
		}
		// 2) 地表以上（山体 / 柱子）填实
		for (int y = seaLevel + 1; y <= top; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (chunk.getBlockState(pos).isAir()) {
				chunk.setBlockState(pos, filler, false);
			}
		}
		// 3) 地表附近的水 / 空洞填成实心
		for (int y = Math.max(minY, top - 3); y <= top; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = chunk.getBlockState(pos);
			if (state.is(Blocks.WATER) || state.isAir()) {
				chunk.setBlockState(pos, filler, false);
			}
		}
		// 4) 顶层
		chunk.setBlockState(new BlockPos(x, top, z), topBlock, false);
	}

	/** 叹息旷野：一望无际的平坦旷野，只有 0~3 格缓起伏，地表灰草 */
	private void buildSighPlain(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		int h = (int) Math.round((valueNoise(x / 70.0, z / 70.0, 0xA11L) + 1.0) * 1.5);
		int top = seaLevel + Math.max(0, Math.min(3, h));
		shapeColumn(chunk, x, z, minY, seaLevel, top,
				XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState(),
				XishenBlocks.GRAY_DIRT.defaultBlockState());
	}

	/**
	 * 苦肉浊林：血肉丛林。
	 *
	 * 按原著"血肉丛生的林间"来做：地面是灰草灰土，上面长**血肉树**——
	 * 树干是血肉原木（5~8 格高），树冠是血肉簇（3×3、两层），树冠边缘再垂下一圈肉块。
	 * 6×6 一格随机长一棵（带偏移，所以不会排成棋盘），密度约 65%。
	 */
	private void buildRotForest(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		int ground = forestGround(x, z, seaLevel);
		shapeColumn(chunk, x, z, minY, seaLevel, ground,
				XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState(),
				XishenBlocks.GRAY_DIRT.defaultBlockState());

		BlockState log = XishenBlocks.FLESH_LOG.defaultBlockState();
		BlockState leaves = XishenBlocks.FLESH_LEAVES.defaultBlockState();
		int spacing = 6;

		for (int cellX = Math.floorDiv(x - 1, spacing); cellX <= Math.floorDiv(x + 1, spacing); cellX++) {
			for (int cellZ = Math.floorDiv(z - 1, spacing); cellZ <= Math.floorDiv(z + 1, spacing); cellZ++) {
				long bits = hashBits(cellX, cellZ, 0x5EED_F1E5L);
				if ((bits & 0xFF) > 165) {
					continue; // 这一格没有树
				}
				int treeX = cellX * spacing + (int) ((bits >>> 8) & 3);
				int treeZ = cellZ * spacing + (int) ((bits >>> 12) & 3);
				int dx = x - treeX;
				int dz = z - treeZ;
				if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
					continue;
				}
				int trunk = 5 + (int) ((bits >>> 16) & 3);
				int base = forestGround(treeX, treeZ, seaLevel);

				if (dx == 0 && dz == 0) {
					for (int y = 1; y <= trunk; y++) {
						chunk.setBlockState(new BlockPos(x, base + y, z), log, false);
					}
				}
				// 树冠：两层 3x3
				setIfAir(chunk, x, base + trunk, z, leaves);
				setIfAir(chunk, x, base + trunk + 1, z, leaves);
				// 树冠下沿垂一圈肉
				if (Math.abs(dx) + Math.abs(dz) >= 1) {
					setIfAir(chunk, x, base + trunk - 1, z, leaves);
				}
			}
		}
	}

	/** 血肉林的地面高度：海平面 + 0~2 格缓起伏 */
	private int forestGround(int x, int z, int seaLevel) {
		int h = (int) Math.round((valueNoise(x / 60.0, z / 60.0, 0xB21L) + 1.0));
		return seaLevel + Math.max(0, Math.min(2, h));
	}

	/** 只有空气才放方块（避免树叶盖掉树干） */
	private void setIfAir(ChunkAccess chunk, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (chunk.getBlockState(pos).isAir()) {
			chunk.setBlockState(pos, state, false);
		}
	}

	/** 位置哈希（用于决定树的位置/高度，确定性） */
	private static long hashBits(int x, int z, long seed) {
		long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		return h;
	}

	/** 虚妄山脉：脊状噪声堆出 0~60 格的山脉，峰顶是灰石、山脚是灰土 */
	private void buildVainRange(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		double ridge = 1.0 - Math.abs(valueNoise(x / 110.0, z / 110.0, 0xC31L));
		double detail = (valueNoise(x / 34.0, z / 34.0, 0xC32L) + 1.0) * 0.5;
		int h = (int) Math.round(Math.pow(ridge, 1.8) * 52.0 + detail * 10.0);
		int top = seaLevel + Math.max(0, h);
		BlockState topBlock = h > 14
				? XishenBlocks.GRAY_STONE.defaultBlockState()
				: XishenBlocks.GRAY_DIRT.defaultBlockState();
		shapeColumn(chunk, x, z, minY, seaLevel, top, topBlock, XishenBlocks.GRAY_STONE.defaultBlockState());
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
