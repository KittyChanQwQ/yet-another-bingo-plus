package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.random.Random

class SpawnService(
    private val log: Logger,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val config: BingoConfig,
    private val teamService: TeamService,
    private val itemStackFactory: IItemStackFactory,
    private val playerManager: IPlayerManager,
    private val spreadPlayersFactory: SpreadPlayers.Factory,
) {

    fun getSpawnDimension(): ServerWorld {
        return server.worlds.find {
            it.registryKey.value.toString() == options.spawnDimension
        } ?: run {
            log.error("[SpawnService] Could not find spawnDimension '${options.spawnDimension}'; defaulting to minecraft:overworld")
            server.overworld
        }
    }

    /**
     * Create team spawnpoints
     */
    fun createSpawnpoints(): CompletableFuture<Unit> {
        val world = getSpawnDimension()
        val spread = spreadPlayersFactory.forWorld(world)
        val teams = state.getRegisteredTeams()

        val distance = options.spawnDistance
        if (distance <= 0) {
            // if the distance is 0, find one spawn point for all teams
            return spread.findSpawnPosAsync(startChunk = Pair(0, 0))
                .thenApply { BlockPosition.fromBlockPos(it) }
                .thenApply { spawnPos ->
                    for (team in teams) {
                        team.spawnpoint = spawnPos
                    }
                }
        } else {
            // spread player teams to spawnpoint
            return spread.spreadAsync(teams, distance)
                .thenApply { spawnpoints ->
                    for (team in teams) {
                        team.spawnpoint = spawnpoints[team.key]
                    }
                }
        }
    }

    fun teleportToLobby(player: ServerPlayerEntity) {
        val lobbyWorld = server.lobbyWorld
        if (lobbyWorld == null) {
            log.error("[SpawnService] Attempted teleportToLobby, but the lobby world is null!")
            return
        }

        val spawn = state.lobbySpawnPos
        player.teleport(lobbyWorld, spawn.x.toDouble(), spawn.y.toDouble(), spawn.z.toDouble(), 180f, 0f)
        player.setSpawnPoint(
            lobbyWorld.registryKey,
            spawn.toBlockPos(),
            0f, true, false,
        )
    }

    fun teleportPlayer(player: ServerPlayerEntity): CompletableFuture<BlockPos?> {
        if (!state.isLobbyMode) {
            log.error("[SpawnService] Attempted teleportPlayer, but isLobbyMode=false!")
            return CompletableFuture.completedFuture(null)
        }

        val world = getSpawnDimension()

        val team = teamService.getPlayerTeam(player)
        val spawnFuture = when {
            team != null -> getTeamSpawnpoint(team)
            else -> CompletableFuture.completedFuture(world.spawnPos)
        }

        return spawnFuture.thenApplyAsync({ spawn ->
            if (spawn == null) {
                log.error("[SpawnService] Attempted teleportPlayer, but got a null value")
                return@thenApplyAsync spawn
            }

            // if the player already has the exact block position, don't teleport them again
            val needsTeleport = player.world != world || player.blockX != spawn.x || player.blockY != spawn.y || player.blockZ != spawn.z
            if (!needsTeleport) return@thenApplyAsync spawn

            // random offset for each player to prevent collision grouping
            player.teleport(world, spawn.x + Random.nextDouble(), spawn.y.toDouble(), spawn.z + Random.nextDouble(), player.yaw, player.pitch)
            player.setSpawnPoint(
                world.registryKey,
                spawn,
                0f, true, false,
            )
            spawn
        }, server)
    }

    /**
     * Finds and records a valid spawnpoint for the provided team
     * @param team - the team to locate a spawnpoint for (team.spawnpoint is assigned when complete)
     * @param sync - whether to run synchronously (blocking) to avoid deadlocks if joined with the server thread
     */
    fun getTeamSpawnpoint(team: BingoTeam, sync: Boolean = false): CompletableFuture<BlockPos?> {
        if (!state.isLobbyMode) {
            log.error("[SpawnService] Attempted getTeamSpawnpoint, but isLobbyMode=false!")
            return CompletableFuture.completedFuture(null)
        }

        val world = getSpawnDimension()

        // Get the farthest existing spawnpoint
        // (in case we need to create a new spawnpoint for the team - this is not thread safe)
        val maxChunk = state.teams.values
            .mapNotNull { it.spawnpoint }
            .maxByOrNull { it.x.absoluteValue }
            ?.toChunkPos()
            ?: Pair(0, 0)

        val storedSpawnpoint = team.spawnpoint

        if (storedSpawnpoint == null) {
            log.warn("[SpawnService] Team ${team.id} does not have a stored spawn point! Finding a new spawn position...")

            val (x, y) = maxChunk

            // Move "spreadPlayersDistance" away from the existing chunk on the x-axis
            val xSign = x.sign.takeIf { it != 0 } ?: 1
            val newSpawnChunk = Pair(x + (options.spawnDistance * xSign), y)
            log.debug("Using new starting chunk {} -> {}", maxChunk, newSpawnChunk)

            // Store the team's new spawn position & return
            val spread = spreadPlayersFactory.forWorld(world)
            val future = when {
                sync -> CompletableFuture.completedFuture(
                    spread.findSpawnPosSync(team.id, newSpawnChunk)
                )
                else -> spread.findSpawnPosAsync(team.id, newSpawnChunk)
            }
            return future.whenComplete { spawnpos, _ ->
                if (spawnpos != null)
                    team.spawnpoint = BlockPosition.fromBlockPos(spawnpos)
            }

        }

        // If the spawnpoint is stored, check that there is space for a player to spawn there
        val pos = storedSpawnpoint.toBlockPos().mutableCopy()
        if (
            !isEmptyBlock(world, pos, world.getBlockState(pos))
            || !isEmptyBlock(world, pos.up(), world.getBlockState(pos.up()))
        ) {
            log.warn("[SpawnService] Spawn position for team $team is obstructed! Finding a new spawn position...")

            val spread = spreadPlayersFactory.forWorld(world)
            val future = when {
                sync -> CompletableFuture.completedFuture(
                    spread.findSpawnPosSync(team.id, storedSpawnpoint.toChunkPos())
                )
                else -> spread.findSpawnPosAsync(team.id, storedSpawnpoint.toChunkPos())
            }
            return future.whenComplete { spawnpos, _ ->
                if (spawnpos != null)
                    team.spawnpoint = BlockPosition.fromBlockPos(spawnpos)
            }
        }

        return CompletableFuture.completedFuture(pos)
    }

    /**
     * Equip the player with any configured spawn equipment
     */
    fun giveSpawnEquipment(players: List<ServerPlayerEntity>) {
        if (!state.isLobbyMode) {
            log.error("[SpawnService] Attempted giveSpawnEquipment, but isLobbyMode=false!")
            return
        }

        if (!options.isPlayerKit) return

        val itemStacks = options.playerKitItems
            .flatMap { it.toItemStacks(itemStackFactory) }

        if (config.preventScoringSpawnKitItems) {
            for (itemStack in itemStacks) {
                // set BINGO_IGNORE flag so that spawn items do not count on the bingo card
                itemStack.addCustomTag(NBT_BINGO_IGNORE)
            }
        }

        for (stack in itemStacks) {
            for (player in players) {
                // copy the stack for each player
                val playerStack = stack.copy()
                // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
                playerManager.forPlayer(player).giveOrEquipStack(playerStack)
            }
        }
    }

}