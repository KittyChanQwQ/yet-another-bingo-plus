package me.jfenn.bingo.client.mixin;

import me.jfenn.bingo.client.impl.draw.DrawService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorTintMixin {
    @ModifyVariable(
            method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
            at = @At("HEAD"),
            ordinal = 4,
            argsOnly = true
    )
    private int tintInnerBlitColor(int color) {
        Integer tint = DrawService.COLOR_TINT;
        if (tint != null) {
            return ARGB.multiply(color, tint);
        } else {
            return color;
        }
    }

    @ModifyVariable(
            method = "innerTiledBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIIIFFFFI)V",
            at = @At("HEAD"),
            ordinal = 6,
            argsOnly = true
    )
    private int tintInnerTiledBlitColor(int color) {
        Integer tint = DrawService.COLOR_TINT;
        if (tint != null) {
            return ARGB.multiply(color, tint);
        } else {
            return color;
        }
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private int tintTextColor(int color) {
        Integer tint = DrawService.COLOR_TINT;
        if (tint != null) {
            return ARGB.multiply(color, tint);
        } else {
            return color;
        }
    }
}
