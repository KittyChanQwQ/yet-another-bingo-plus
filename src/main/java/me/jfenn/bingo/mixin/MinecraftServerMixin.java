package me.jfenn.bingo.mixin;

import me.jfenn.bingo.BingoGame;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    Logger log = LoggerFactory.getLogger(MinecraftServerMixin.class);

    @Inject(at = @At(value = "HEAD"), method = "prepareStartRegion", cancellable = true)
    private void prepareStartRegion(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        // The start region worldgen isn't all that useful, since the game starts at random coords anyway. So it doesn't really help.
        ci.cancel();
    }

    @Shadow
    private boolean saving;

    @Shadow @Final
    private ServerNetworkIo networkIo;

    @Shadow
    private PlayerManager playerManager;

    @Shadow
    public Iterable<ServerWorld> getWorlds() {
        throw new IllegalStateException();
    }

    @Shadow
    public ResourceManager getResourceManager() {
        throw new IllegalStateException();
    }

    @Shadow @Final
    private LevelStorage.Session session;

    @Inject(at = @At(value = "HEAD"), method = "shutdown", cancellable = true)
    public void shutdown(CallbackInfo ci) {
        // tell Fabric to invoke SERVER_STOPPING, which *might* not be the first mixin
        ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(this.playerManager.getServer());

        // many of the shutdown tasks can be skipped, since we don't care if all the world data is saved
        log.info("Stopping server");

        if (this.networkIo != null) {
            this.networkIo.stop();
        }

        this.saving = true;

        if (this.playerManager != null) {
            log.info("Disconnecting players");
            this.playerManager.disconnectAllPlayers();
        }

        if (BingoGame.INSTANCE.getConfig().getUnsafeSkipWorldClose()) {
            log.info("unsafeSkipWorldClose is true; skipping file closing");
            log.info("This will likely cause a crash...");

            this.saving = false;
            // tell Fabric to invoke SERVER_STOPPED (otherwise this would not reach its mixin)
            ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(this.playerManager.getServer());
            ci.cancel();
            return;
        }

        log.info("Closing worlds");
        for (ServerWorld serverWorld : this.getWorlds()) {
            if (serverWorld == null) continue;
            try {
                serverWorld.close();
            } catch (IOException e) {
                log.error("Exception closing the level", e);
            }
        }

        this.saving = false;

        var resourceManager = this.getResourceManager();
        if (resourceManager instanceof LifecycledResourceManager)
            ((LifecycledResourceManager) resourceManager).close();

        try {
            this.session.close();
        } catch (IOException e) {
            log.error("Exception closing the session", e);
        }

        // tell Fabric to invoke SERVER_STOPPED (otherwise this would not reach its mixin)
        ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(this.playerManager.getServer());
        ci.cancel();
    }

}
