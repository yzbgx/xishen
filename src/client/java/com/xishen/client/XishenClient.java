package com.xishen.client;

import com.xishen.entity.XishenEntities;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * 仅客户端的入口：渲染、按键绑定、HUD 之类只存在于客户端的逻辑写在这里。
 * 这里只能 import 客户端类，不能引用仅服务端存在的类。
 */
public class XishenClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 血肉船的渲染器
		EntityRendererRegistry.register(XishenEntities.FLESH_BOAT, FleshBoatRenderer::new);
	}
}
