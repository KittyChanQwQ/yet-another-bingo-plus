package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object PlayerManagerMixinHandler {

    fun setPregameSpawnData(player: ServerPlayerEntity, ci: CallbackInfoReturnable<NbtCompound>) {
        val scope = BingoKoin.getScope(player.server) ?: return
        val state = scope.get<BingoState>()

        // Provide default player data (for the lobby dimension / spawnpoint) if null
        if (ci.returnValue == null && state.state == GameState.PREGAME) {
            val nbt = NbtCompound()
            nbt.putString("Dimension", LOBBY_WORLD_ID.toString())

            val spawnPos: BlockPos = state.lobbySpawnPos.toBlockPos()

            val position = NbtList()
            position.add(NbtDouble.of(spawnPos.x + 0.5))
            position.add(NbtDouble.of(spawnPos.y.toDouble()))
            position.add(NbtDouble.of(spawnPos.z + 0.5))
            nbt.put("Pos", position)

            val rotation = NbtList()
            rotation.add(NbtFloat.of(180f)) // yaw
            rotation.add(NbtFloat.of(0f)) // pitch
            nbt.put("Rotation", rotation)

            player.readNbt(nbt)
            ci.setReturnValue(nbt)
        }
    }

    fun isEndSpawnDimension(server: MinecraftServer): Boolean {
        val scope = BingoKoin.getScope(server) ?: run {
            println("BUG: PlayerManagerMixinHandler.isEndSpawnDimension")
            return false
        }

        val spawnService = scope.get<SpawnService>()
        val dimension = spawnService.getSpawnDimension()
        return dimension.registryKey.value.path == "the_end"
    }

    fun setPlayerSpawnpoint(player: ServerPlayerEntity) {
        val scope = BingoKoin.getScope(player.server) ?: return
        val spawnService = scope.get<SpawnService>()
        val teamService = scope.get<TeamService>()

        // find the player's bingo team spawnpoint, if possible
        val team: BingoTeam = teamService.getPlayerTeam(player) ?: return
        val spawn: BlockPos = spawnService.getTeamSpawnpoint(team) ?: return

        // change the player's spawnpoint back to the team spawn location
        val dimension = spawnService.getSpawnDimension()
        player.setSpawnPoint(dimension.registryKey, spawn, 0f, true, false)
    }

}