package com.xishen.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 通用 Mixin 示例（现在什么都不做）。
 * Mixin 用来往 Minecraft 原版代码里插入自己的逻辑，配置见 xishen.mixins.json。
 */
@Mixin(MinecraftServer.class)
public class XishenMixin {
	@Inject(at = @At("HEAD"), method = "loadLevel")
	private void init(CallbackInfo info) {
		// 这段代码会注入到 MinecraftServer.loadLevel() 的开头
	}
}
