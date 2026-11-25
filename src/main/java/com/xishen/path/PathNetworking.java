package com.xishen.path;

import com.xishen.XishenMod;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * 加点的网络包：客户端点「+」→ 发这个包 → 服务端校验并扣点升级
 * （数据是同步的附件，服务端改完会自动同步回客户端）。
 */
public final class PathNetworking {
	private PathNetworking() {
	}

	/** C2S：给某条神道加一级 */
	public record SpendPoint(String pathId) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SpendPoint> TYPE =
				new CustomPacketPayload.Type<>(XishenMod.id("spend_point"));

		public static final StreamCodec<RegistryFriendlyByteBuf, SpendPoint> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, SpendPoint::pathId,
				SpendPoint::new);

		@Override
		public CustomPacketPayload.Type<SpendPoint> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SpendPoint.TYPE, SpendPoint.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SpendPoint.TYPE, (payload, context) ->
				context.server().execute(() -> spend(context.player(), payload.pathId())));
	}

	/** 每次点「+」把 1 点加点换成这么多熟练度 */
	public static final int PROFICIENCY_PER_POINT = 10;

	/** 服务端校验：有剩余点数、熟练度没满、神道存在 → 扣 1 点加点、熟练度 +10 */
	private static void spend(ServerPlayer player, String pathId) {
		PathType type = PathType.byId(pathId);
		if (type == null) {
			return;
		}
		PathData data = PathAttachments.get(player);
		if (data.points() <= 0 || data.proficiency(type) >= PathType.MAX_PROFICIENCY) {
			return;
		}
		PathData updated = data.addProficiency(type, PROFICIENCY_PER_POINT).withPoints(data.points() - 1);
		PathAttachments.set(player, updated);
	}
}
