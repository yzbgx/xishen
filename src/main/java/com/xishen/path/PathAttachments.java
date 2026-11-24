package com.xishen.path;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.world.entity.player.Player;

/**
 * 神道数据的附件注册：持久化 + 死亡不掉 + 自动同步给客户端。
 */
public final class PathAttachments {
	private PathAttachments() {
	}

	public static final AttachmentType<PathData> PATH_DATA = AttachmentRegistry.<PathData>builder()
			.persistent(PathData.CODEC)
			.copyOnDeath()
			.syncWith(PathData.STREAM_CODEC, AttachmentSyncPredicate.all())
			.initializer(PathData::empty)
			.buildAndRegister(XishenMod.id("path_data"));

	public static void register() {
		// 触发类加载
	}

	/** 取玩家数据，没有就创建默认值 */
	public static PathData get(Player player) {
		return player.getAttachedOrCreate(PATH_DATA);
	}

	/** 写回玩家数据 */
	public static void set(Player player, PathData data) {
		player.setAttached(PATH_DATA, data);
	}
}
