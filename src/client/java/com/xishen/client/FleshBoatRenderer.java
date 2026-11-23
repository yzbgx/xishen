package com.xishen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import com.xishen.entity.FleshBoat;

import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * 血肉船的渲染器：模型直接复用原版船的模型（{@link BoatModel}），只把贴图换成血肉纹理。
 * 变换（上下浮动、受伤摇晃、水面遮罩）都照原版 {@code BoatRenderer} 写。
 */
public class FleshBoatRenderer extends EntityRenderer<FleshBoat> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath("xishen", "textures/entity/boat/flesh_boat.png");

	private final BoatModel model;

	public FleshBoatRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new BoatModel(context.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK)));
		this.shadowRadius = 0.8F;
	}

	@Override
	public void render(FleshBoat boat, float yaw, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int light) {
		pose.pushPose();
		pose.translate(0.0F, 0.375F, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));

		float hurtTime = boat.getHurtTime() - partialTick;
		float damage = boat.getDamage() - partialTick;
		if (damage > 0.0F) {
			hurtTime = damage;
		}
		if (hurtTime > 0.0F) {
			pose.mulPose(Axis.XP.rotationDegrees(
					Mth.sin(hurtTime) * hurtTime * damage / 10.0F * (float) boat.getHurtDir()));
		}

		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.mulPose(Axis.YP.rotationDegrees(90.0F));

		this.model.setupAnim(boat, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);
		VertexConsumer body = buffers.getBuffer(this.model.renderType(TEXTURE));
		this.model.renderToBuffer(pose, body, light, OverlayTexture.NO_OVERLAY);

		if (!boat.isUnderWater()) {
			VertexConsumer patch = buffers.getBuffer(RenderType.waterMask());
			this.model.waterPatch().render(pose, patch, light, OverlayTexture.NO_OVERLAY);
		}

		pose.popPose();
		super.render(boat, yaw, partialTick, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(FleshBoat boat) {
		return TEXTURE;
	}
}
