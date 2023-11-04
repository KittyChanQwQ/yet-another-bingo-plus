package me.jfenn.bingo.mixin;

import me.jfenn.bingo.BingoGame;
import me.jfenn.bingo.GameState;
import me.jfenn.bingo.card.BingoTeam;
import me.jfenn.bingo.menu.MenuService;
import me.jfenn.bingo.spawn.SpawnService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(at = @At(value = "RETURN"), method = "loadPlayerData", cancellable = true)
    private void loadPlayerData(ServerPlayerEntity player, CallbackInfoReturnable<NbtCompound> ci) {
        // Provide default player data (for the lobby dimension / spawnpoint) if null
        if (ci.getReturnValue() == null && BingoGame.INSTANCE.getState() == GameState.PREGAME) {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("Dimension", "bingo:lobby");

            BlockPos spawnPos = MenuService.INSTANCE.getSpawnPos();

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

            BlockPos spawn = SpawnService.INSTANCE.getTeamSpawnpoints().get(team);
            if (spawn == null) return;

            // change the player's spawnpoint back to the team spawn location
            player.setSpawnPoint(player.getServer().getOverworld().getRegistryKey(), spawn, 0f, true, false);
        }
    }
}
