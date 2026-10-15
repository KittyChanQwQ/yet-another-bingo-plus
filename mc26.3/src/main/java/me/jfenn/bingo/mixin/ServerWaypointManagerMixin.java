package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhandler.LocatorBarMixinHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filters locator bar (waypoint) connections based on the selected LocatorBarMode:
 * every player, only your team (+ the leading team), or only your team.
 */
@Mixin(ServerWaypointManager.class)
public class ServerWaypointManagerMixin {

    @Inject(
            at = @At("HEAD"),
            method = "createConnection",
            cancellable = true
    )
    private void onCreateConnection(ServerPlayer receiver, WaypointTransmitter transmitter, CallbackInfo ci) {
        if (transmitter instanceof ServerPlayer player) {
            if (!LocatorBarMixinHelper.Companion.shouldCreateConnection(receiver, player)) {
                ci.cancel();
            }
        }
    }
}
