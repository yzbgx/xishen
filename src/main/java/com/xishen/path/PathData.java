package com.xishen.path;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家的神道数据：14 条神道各自的等级 + 剩余加点。
 *
 * <p>持久化用 {@link #CODEC}（存进玩家 NBT，死亡不掉落），
 * 网络同步用 {@link #STREAM_CODEC}（Fabric 的 attachment 自动同步）。
 */
public record PathData(List<Integer> levels, int points) {
	public static final Codec<PathData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.listOf().fieldOf("levels").forGetter(PathData::levels),
			Codec.INT.fieldOf("points").forGetter(PathData::points)
	).apply(instance, PathData::new));

	public static final StreamCodec<FriendlyByteBuf, PathData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				buf.writeVarInt(data.points());
				for (int i = 0; i < PathType.COUNT; i++) {
					buf.writeVarInt(data.level(i));
				}
			},
			buf -> {
				int points = buf.readVarInt();
				List<Integer> levels = new ArrayList<>(PathType.COUNT);
				for (int i = 0; i < PathType.COUNT; i++) {
					levels.add(buf.readVarInt());
				}
				return new PathData(Collections.unmodifiableList(levels), points);
			});

	/** 全新的空数据：14 条都是 0 级、0 点 */
	public static PathData empty() {
		List<Integer> levels = new ArrayList<>(PathType.COUNT);
		for (int i = 0; i < PathType.COUNT; i++) {
			levels.add(0);
		}
		return new PathData(Collections.unmodifiableList(levels), 0);
	}

	public int level(PathType type) {
		return this.level(type.ordinal());
	}

	public int level(int index) {
		if (index < 0 || index >= this.levels.size()) {
			return 0;
		}
		return this.levels.get(index);
	}

	public PathData withLevel(PathType type, int value) {
		List<Integer> copy = new ArrayList<>(this.levels);
		while (copy.size() < PathType.COUNT) {
			copy.add(0);
		}
		copy.set(type.ordinal(), Math.max(0, Math.min(PathType.MAX_LEVEL, value)));
		return new PathData(Collections.unmodifiableList(copy), this.points);
	}

	public PathData withPoints(int value) {
		return new PathData(this.levels, Math.max(0, value));
	}
}
