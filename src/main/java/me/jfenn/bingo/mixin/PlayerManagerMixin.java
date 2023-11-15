package me.jfenn.bingo.mixin;

import me.jfenn.bingo.Bingo;
import me.jfenn.bingo.state.GameState;
import me.jfenn.bingo.card.BingoTeam;
import me.jfenn.bingo.spawn.SpawnService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow @Final private MinecraftServer server;
    Logger log = LoggerFactory.getLogger(PlayerManagerMixin.class);

    @Inject(at = @At(value = "RETURN"), method = "loadPlayerData", cancellable = true)
    private void loadPlayerData(ServerPlayerEntity player, CallbackInfoReturnable<NbtCompound> ci) {
        var ctx = Bingo.INSTANCE.getContext(player.server);
        if (ctx == null) return;

        // Provide default player data (for the lobby dimension / spawnpoint) if null
        if (ci.getReturnValue() == null && ctx.getState().getState() == GameState.PREGAME) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("Dimension", "bingo:lobby");

            BlockPos spawnPos = ctx.getState().getLobbySpawnPos().toBlockPos();

            NbtList position = new NbtList();
            position.add(NbtDouble.of(spawnPos.getX() + 0.5));
            position.add(NbtDouble.of(spawnPos.getY()));
            position.add(NbtDouble.of(spawnPos.getZ() + 0.5));
            nbt.put("Pos", position);

            NbtList rotation = new NbtList();
            rotation.add(NbtFloat.of(180f)); // yaw
            rotation.add(NbtFloat.of(0f)); // pitch
            nbt.put("Rotation", rotation);

            player.readNbt(nbt);
            ci.setReturnValue(nbt);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "respawnPlayer")
    private void respawnPlayer(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> info) {
        var ctx = Bingo.INSTANCE.getContext(player.server);
        if (ctx == null) return;

        if (alive) {
            // the player is exiting through the end portal...
            var dimension = SpawnService.INSTANCE.getSpawnDimension(ctx);
            // if the spawnpoint is also in the end, then respawn them in the overworld
            if (dimension.getRegistryKey().getValue().getPath().equals("the_end")) {
                var overworld = player.server.getOverworld();
                player.setSpawnPoint(overworld.getRegistryKey(), overworld.getSpawnPos(), overworld.getSpawnAngle(), false, false);
            }
            return;
        }

        // this can be null if the player's previous spawnpoint failed... so it should default to the origin
        Optional<Vec3d> targetRespawn = Optional.empty();
        try {
            var spawnWorld = player.server.getWorld(player.getSpawnPointDimension());
            var spawnPosition = player.getSpawnPointPosition();
            targetRespawn = PlayerEntity.findRespawnPosition(
                    spawnWorld != null ? spawnWorld : server.getOverworld(),
                    spawnPosition != null ? spawnPosition : BlockPos.ORIGIN,
                    player.getSpawnAngle(),
                    player.isSpawnForced(),
                    alive
            );
        } catch (Throwable e) {
            log.error("Exception thrown during PlayerEntity.findRespawnPosition", e);
        }

        // if the targeted spawnpoint is invalid (i.e. a broken bed, or otherwise somewhere that would reset to the world spawn) ...
        if (targetRespawn.isEmpty()) {
            // find the player's bingo team spawnpoint, if possible
            BingoTeam team = BingoTeam.Companion.fromPlayer(player);
            if (team == null) return;

            BlockPos spawn = SpawnService.INSTANCE.getTeamSpawnpoint(ctx, team);
            if (spawn == null) return;

            // change the player's spawnpoint back to the team spawn location
            var dimension = SpawnService.INSTANCE.getSpawnDimension(ctx);
            player.setSpawnPoint(dimension.getRegistryKey(), spawn, 0f, true, false);
        }
    }
}
