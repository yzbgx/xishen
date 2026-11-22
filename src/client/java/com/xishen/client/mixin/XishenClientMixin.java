package com.xishen.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端 Mixin 示例（现在什么都不做），配置见 xishen.client.mixins.json。
 */
@Mixin(Minecraft.class)
public class XishenClientMixin {
	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
		// 这段代码会注入到 Minecraft.run() 的开头
	}
}
