package com.xishen.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * 仅客户端的入口：渲染、按键绑定、HUD 之类只存在于客户端的逻辑写在这里。
 * 这里只能 import 客户端类，不能引用仅服务端存在的类。
 */
public class XishenClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}
}
