package me.jfenn.bingo.client.mixin;

import me.jfenn.bingo.client.impl.screen.ScreenImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "extractOptions()V",
            at = @At("TAIL")
    )
    void yamb$extractOptions(CallbackInfo ci) {
        if (minecraft.gui.screen() instanceof ScreenImpl screen) {
            float blurriness = screen.getBlurIntensity() * gameRenderState.optionsRenderState.menuBackgroundBlurriness;
            gameRenderState.optionsRenderState.menuBackgroundBlurriness = Math.round(blurriness);
        }
    }
}
