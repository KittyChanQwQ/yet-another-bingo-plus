package me.jfenn.bingo.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(at = @At(value = "HEAD"), method = "prepareStartRegion", cancellable = true)
    private void prepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        // The start region worldgen isn't all that useful, since the game starts at random coords anyway. So it doesn't really help.
        ci.cancel();
    }

}
