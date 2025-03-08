package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.scope.BingoKoin
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

object PlayerManagerMixinHandler {

    fun shouldPreventLobbyChaos(server: MinecraftServer): Boolean {
        val scope = BingoKoin.getScope(server) ?: return false
        val config = scope.get<BingoConfig>()
        val state = scope.get<BingoState>()

        return state.isLobbyMode &&
                config.preventLobbyChaos &&
                state.state == GameState.PREGAME
    }

    fun shouldSpawnInLobby(server: MinecraftServer): Boolean {
        val scope = BingoKoin.getScope(server) ?: return false
        val state = scope.get<BingoState>()
        return state.isLobbyMode && (state.state == GameState.PREGAME || state.state == GameState.PRELOADING)
    }

    fun getPregameSpawnData(server: MinecraftServer): NbtCompound? {
        val scope = BingoKoin.getScope(server) ?: return null
        val state = scope.get<BingoState>()

        val nbt = NbtCompound()
        nbt.putString("Dimension", LOBBY_WORLD_ID.toString())

        val spawnPos: BlockPos = state.lobbySpawnPos.toBlockPos()

        val position = NbtList()
        position.add(NbtDouble.of(spawnPos.x + 0.5))
        position.add(NbtDouble.of(spawnPos.y.toDouble()))
        position.add(NbtDouble.of(spawnPos.z + 0.5))
        nbt.put("Pos", position)

        val rotation = NbtList()
        rotation.add(NbtFloat.of(state.lobbySpawnYaw)) // yaw
        rotation.add(NbtFloat.of(0f)) // pitch
        nbt.put("Rotation", rotation)
        return nbt
    }

    fun shouldOverrideEndRespawn(server: MinecraftServer): Boolean {
        val scope = BingoKoin.getScope(server) ?: run {
            println("BUG: PlayerManagerMixinHandler.isEndSpawnDimension")
            return false
        }

        val state = scope.get<BingoState>()
        val spawnService = scope.get<SpawnService>()
        val dimension = spawnService.getSpawnDimension()
        return state.isLobbyMode && dimension.identifier.substringAfter(':') == "the_end"
    }

    fun setPlayerSpawnpoint(player: ServerPlayerEntity) {
        val scope = BingoKoin.getScope(player.server) ?: return
        val state = scope.get<BingoState>()
        val spawnService = scope.get<SpawnService>()
        val teamService = scope.get<TeamService>()
        val playerManager = scope.get<IPlayerManager>()

        if (!state.isLobbyMode) {
            // if isLobbyMode=false, mixins should never overwrite the player spawnpoint
            return
        }

        // find the player's bingo team spawnpoint, if possible
        val team: BingoTeam = teamService.getPlayerTeam(player) ?: return
        val spawn: BlockPos = spawnService.getTeamSpawnpoint(team)?.toBlockPos() ?: return

        // change the player's spawnpoint back to the team spawn location
        val dimension = spawnService.getSpawnDimension()
        playerManager.forPlayer(player).setSpawnPoint(
            world = dimension,
            spawn = BlockPosition.fromBlockPos(spawn),
            angle = 0f,
            forced = true,
            sendMessage = false,
        )
    }

}