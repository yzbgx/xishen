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
	 * 鬼嘲深渊：一条贯穿区域的**大峡谷**（不是圆坑）。
	 *
	 * 走向由区域中心的哈希决定（每个世界固定一条），中心线沿走向来回摆动，宽度 24~42 格；
	 * 中间最深约 96 格（谷底 y≈0），两端逐渐收浅，谷壁按 4 格一台阶形成陡崖；
	 * 谷底铺灰土 / 零散灰砾石，峡谷之外是灰草台地。
	 */
	private void carveAbyss(ChunkAccess chunk, GrayRealmLayout layout, int x, int z, int minY, int seaLevel) {
		GrayRealmLayout.PlacedRegion abyss = layout.regionById("ghost_abyss");
		if (abyss == null) {
			return;
		}
		BlockState stone = XishenBlocks.GRAY_STONE.defaultBlockState();
		BlockState dirt = XishenBlocks.GRAY_DIRT.defaultBlockState();
		BlockState grass = XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		double dx = x - abyss.centerX();
		double dz = z - abyss.centerZ();
		double radius = abyss.radius();

		// 峡谷走向（用区域中心做种子，每个世界/区域固定一条）
		double angle = (hash(0xAB1L, (int) abyss.centerX(), (int) abyss.centerZ()) + 1.0) * Math.PI;
		double ux = Math.cos(angle);
		double uz = Math.sin(angle);
		double along = dx * ux + dz * uz;
		double perp = -dx * uz + dz * ux;

		// 峡谷外的台地地面
		int ground = seaLevel + (int) Math.round(valueNoise(x / 80.0, z / 80.0, 0xAB2L) * 2.0);

		// 中心线摆动 + 宽度变化
		double wobble = valueNoise(along / 80.0, 7.0, 0xAB3L) * 42.0;
		double halfWidth = 24.0 + (valueNoise(along / 140.0, 31.0, 0xAB4L) + 1.0) * 9.0;
		double dist = Math.abs(perp - wobble);

		double taper = Math.max(0.0, 1.0 - Math.pow(Math.min(1.0, Math.abs(along) / (radius * 1.05)), 2.0));
		double depth = 0.0;
		if (dist < halfWidth) {
			double side = Math.cos((dist / halfWidth) * Math.PI / 2.0);
			depth = 96.0 * taper * Math.pow(side, 0.75);
		}

		if (depth < 2.0) {
			shapeColumn(chunk, x, z, minY, seaLevel, ground, grass, dirt);
			return;
		}

		int floor = (int) Math.round((seaLevel - depth) / 4.0) * 4;

		for (int y = minY; y <= floor; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState current = chunk.getBlockState(pos);
			if (current.isAir() || !current.getFluidState().isEmpty()) {
				chunk.setBlockState(pos, stone, false);
			}
		}
		int clearTop = Math.max(ground, seaLevel) + 8;
		for (int y = floor + 1; y <= clearTop; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!chunk.getBlockState(pos).isAir()) {
				chunk.setBlockState(pos, air, false);
			}
		}
		BlockState floorTop = valueNoise(x / 9.0, z / 9.0, 0xAB5L) > 0.45
				? XishenBlocks.GRAY_GRAVEL.defaultBlockState()
				: dirt;
		chunk.setBlockState(new BlockPos(x, floor, z), floorTop, false);
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

	/**
	 * 叹息旷野：一望无际的旷野。
	 *
	 * 和"灰雾带"（纯平）区分开：这里有 0~3 格缓起伏，还散布着**浅坑洼地**（风蚀地貌），
	 * 让地面看上去有起伏和层次，而不是一张灰纸。
	 */
	private void buildSighPlain(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		double n = valueNoise(x / 70.0, z / 70.0, 0xA11L) * 0.7
				+ valueNoise(x / 28.0, z / 28.0, 0xA12L) * 0.3;
		int top = seaLevel + Math.max(0, Math.min(3, (int) Math.round((n + 1.0) * 1.8)));

		int cell = 96;
		int cellX = Math.floorDiv(x, cell);
		int cellZ = Math.floorDiv(z, cell);
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				long bits = hashBits(cellX + ox, cellZ + oz, 0x9A11L);
				if ((bits & 0xFF) > 110) {
					continue;
				}
				double px = (cellX + ox) * cell + (bits >>> 8 & 63);
				double pz = (cellZ + oz) * cell + (bits >>> 16 & 63);
				double r = 10.0 + (bits >>> 24 & 15);
				double d = Math.hypot(x - px, z - pz);
				if (d < r) {
					int dip = (int) Math.round(5.0 * Math.cos((d / r) * Math.PI / 2.0));
					top -= dip;
				}
			}
		}

		shapeColumn(chunk, x, z, minY, seaLevel, top,
				XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState(),
				XishenBlocks.GRAY_DIRT.defaultBlockState());
	}

	/**
	 * 苦肉浊林：血肉丛林的地面（树由 biome 里的 tree 特征生成，见 data/xishen/worldgen/configured_feature）。
	 *
	 * 这里只做地表：灰草 + 0~2 格缓起伏，给树留出生长空间。
	 */
	private void buildRotForest(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		int ground = forestGround(x, z, seaLevel);
		shapeColumn(chunk, x, z, minY, seaLevel, ground,
				XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState(),
				XishenBlocks.GRAY_DIRT.defaultBlockState());
	}

	/** 血肉林的地面高度：海平面 + 0~2 格缓起伏 */
	private int forestGround(int x, int z, int seaLevel) {
		int h = (int) Math.round((valueNoise(x / 60.0, z / 60.0, 0xB21L) + 1.0));
		return seaLevel + Math.max(0, Math.min(2, h));
	}

	/** 位置哈希（用于决定树的位置/高度，确定性） */
	private static long hashBits(int x, int z, long seed) {
		long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		return h;
	}

	/**
	 * 虚妄山脉：脊状噪声堆出 0~60 格的山脉。
	 *
	 * 表层按**坡度**决定：陡壁（相邻高度差 ≥6）露灰石，缓坡和盆地一律铺灰草，
	 * 所以不管多低的盆地都有土层，只有悬崖才露岩石。
	 */
	private void buildVainRange(ChunkAccess chunk, int x, int z, int minY, int seaLevel) {
		int h = vainHeight(x, z);
		int top = seaLevel + h;
		int slope = Math.max(Math.max(Math.abs(h - vainHeight(x + 3, z)), Math.abs(h - vainHeight(x - 3, z))),
				Math.max(Math.abs(h - vainHeight(x, z + 3)), Math.abs(h - vainHeight(x, z - 3))));
		BlockState topBlock = slope >= 6
				? XishenBlocks.GRAY_STONE.defaultBlockState()
				: XishenBlocks.GRAY_GRASS_BLOCK.defaultBlockState();
		shapeColumn(chunk, x, z, minY, seaLevel, top, topBlock, XishenBlocks.GRAY_STONE.defaultBlockState());
	}

	/** 虚妄山脉的高度（不含海平面） */
	private static int vainHeight(int x, int z) {
		double ridge = 1.0 - Math.abs(valueNoise(x / 110.0, z / 110.0, 0xC31L));
		double detail = (valueNoise(x / 34.0, z / 34.0, 0xC32L) + 1.0) * 0.5;
		return Math.max(0, (int) Math.round(Math.pow(ridge, 1.8) * 52.0 + detail * 10.0));
	}

	/**
	 * 灰雾带的起伏：**纯平**（0 格）。
	 *
	 * 灰雾带是"什么都没有"的连接地带，平得像一张灰纸；
	 * 起伏留给"叹息旷野"，山留给"虚妄山脉"。
	 */
	private static int hillHeight(int x, int z) {
		return 0;
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
