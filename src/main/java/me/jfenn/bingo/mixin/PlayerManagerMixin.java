package me.jfenn.bingo.mixin;

import me.jfenn.bingo.BingoGame;
import me.jfenn.bingo.card.BingoTeam;
import me.jfenn.bingo.event.PlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(at = @At(value = "TAIL"), method = "onPlayerConnect")
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
        PlayerEvents.INSTANCE.getOnPlayerJoin().invoke(player);
    }

    @Inject(at = @At(value = "TAIL"), method = "remove")
    private void onPlayerLeave(ServerPlayerEntity player, CallbackInfo info) {
        PlayerEvents.INSTANCE.getOnPlayerLeave().invoke(player);
    }

    @Inject(at = @At(value = "HEAD"), method = "respawnPlayer")
    private void respawnPlayer(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> info) {
        if (player.isSpawnForced()) return;

        Optional<Vec3d> targetRespawn = PlayerEntity.findRespawnPosition(
                player.server.getWorld(player.getSpawnPointDimension()),
                player.getSpawnPointPosition(),
                player.getSpawnAngle(),
                player.isSpawnForced(),
                alive
        );

        // if the targeted spawnpoint is invalid (i.e. a broken bed, or otherwise somewhere that would reset to the world spawn) ...
        if (targetRespawn.isEmpty()) {
            // find the player's bingo team spawnpoint, if possible
            BingoTeam team = BingoTeam.Companion.fromPlayer(player);
            if (team == null) return;

            BlockPos spawn = BingoGame.INSTANCE.getTeamSpawnpoints().get(team);
            if (spawn == null) return;

            // change the player's spawnpoint back to the team spawn location
            player.setSpawnPoint(player.getServer().getOverworld().getRegistryKey(), spawn, 0f, true, false);
        }
    }
}
