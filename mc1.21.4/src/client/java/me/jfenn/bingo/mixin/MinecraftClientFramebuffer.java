package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhelper.FramebufferOverride;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientFramebuffer {
    @Inject(
            at = @At(value = "HEAD"),
            method = "getFramebuffer()Lnet/minecraft/client/gl/Framebuffer;",
            cancellable = true
    )
    void getFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
        Framebuffer framebuffer = FramebufferOverride.INSTANCE.getFramebuffer();
        if (framebuffer != null) {
            ci.setReturnValue(framebuffer);
            ci.cancel();
        }
    }
}
