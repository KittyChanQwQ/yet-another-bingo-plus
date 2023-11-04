package me.jfenn.bingo.mixin.skip_world_close;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(ServerWorld.class)
public abstract class ThreadedAnvilChunkStorageMixin {

    @Inject(at = @At(value = "HEAD"), method = "shutdown", cancellable = true)
    public void close(CallbackInfo ci) throws IOException {
        // just return and do nothing - we don't care about fully writing save data
        // since it'll be deleted anyway...
        ci.cancel();
    }

}
