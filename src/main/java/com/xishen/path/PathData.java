package com.xishen.path;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家的神道数据：14 条神道各自的**职业熟练度** + 剩余加点。
 *
 * <p>熟练度 0 ~ 900：
 * <ul>
 *   <li>0~99：未激活</li>
 *   <li>100：激活该神道（以后接"神道星邀请"）</li>
 *   <li>每 +100 = +1 阶，最高 9 阶</li>
 * </ul>
 */
public record PathData(List<Integer> proficiency, int points) {
	public static final Codec<PathData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.listOf().optionalFieldOf("proficiency", Collections.emptyList()).forGetter(PathData::proficiency),
			Codec.INT.optionalFieldOf("points", 0).forGetter(PathData::points)
	).apply(instance, PathData::new));

	public static final StreamCodec<FriendlyByteBuf, PathData> STREAM_CODEC = StreamCodec.of(
			(buf, data) -> {
				buf.writeVarInt(data.points());
				for (int i = 0; i < PathType.COUNT; i++) {
					buf.writeVarInt(data.proficiency(i));
				}
			},
			buf -> {
				int points = buf.readVarInt();
				List<Integer> values = new ArrayList<>(PathType.COUNT);
				for (int i = 0; i < PathType.COUNT; i++) {
					values.add(buf.readVarInt());
				}
				return new PathData(Collections.unmodifiableList(values), points);
			});

	/** 全新的空数据：14 条熟练度都是 0、0 点加点 */
	public static PathData empty() {
		List<Integer> values = new ArrayList<>(PathType.COUNT);
		for (int i = 0; i < PathType.COUNT; i++) {
			values.add(0);
		}
		return new PathData(Collections.unmodifiableList(values), 0);
	}

	public int proficiency(PathType type) {
		return this.proficiency(type.ordinal());
	}

	public int proficiency(int index) {
		if (index < 0 || index >= this.proficiency.size()) {
			return 0;
		}
		return this.proficiency.get(index);
	}

	/** 0 = 未激活，1~9 = 阶数 */
	public int level(PathType type) {
		return PathType.levelFor(this.proficiency(type));
	}

	public boolean activated(PathType type) {
		return PathType.isActivated(this.proficiency(type));
	}

	public PathData withProficiency(PathType type, int value) {
		List<Integer> copy = new ArrayList<>(this.proficiency);
		while (copy.size() < PathType.COUNT) {
			copy.add(0);
		}
		copy.set(type.ordinal(), Math.max(0, Math.min(PathType.MAX_PROFICIENCY, value)));
		return new PathData(Collections.unmodifiableList(copy), this.points);
	}

	/** 加熟练度（返回新数据，自动夹在上限内） */
	public PathData addProficiency(PathType type, int amount) {
		return withProficiency(type, this.proficiency(type) + amount);
	}

	public PathData withPoints(int value) {
		return new PathData(this.proficiency, Math.max(0, value));
	}
}
