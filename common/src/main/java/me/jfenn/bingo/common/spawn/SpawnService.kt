package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.giveOrEquipStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import org.slf4j.Logger
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
    fun createSpawnpoints() {
        val world = getSpawnDimension()
        val spread = SpreadPlayers(world)

        val distance = options.spawnDistance
        if (distance <= 0) {
            // if the distance is 0, find one spawn point for all teams
            val spawnPos = spread.findSpawnPos(Pair(0, 0))
                .let { BlockPosition.fromBlockPos(it) }

            for (team in state.getRegisteredTeams()) {
                team.spawnpoint = spawnPos
            }
        } else {
            // spread player teams to spawnpoint
            spread.spread(state.getRegisteredTeams(), distance)
        }
    }

    fun teleportToLobby(player: ServerPlayerEntity) {
        val spawn = state.lobbySpawnPos
        val lobbyWorld = server.lobbyWorld!!
        player.teleport(lobbyWorld, spawn.x.toDouble(), spawn.y.toDouble(), spawn.z.toDouble(), 180f, 0f)
        player.setSpawnPoint(
            lobbyWorld.registryKey,
            spawn.toBlockPos(),
            0f, true, false,
        )
    }

    fun teleportPlayer(player: ServerPlayerEntity) {
        val world = getSpawnDimension()

        val team = teamService.getPlayerTeam(player)
        val spawn = team?.let {
            getTeamSpawnpoint(team)
        } ?: run {
            world.spawnPos
        }

        // if the player already has the exact block position, don't teleport them again
        val needsTeleport = player.world != world || player.blockX != spawn.x || player.blockY != spawn.y || player.blockZ != spawn.z
        if (!needsTeleport) return

        // random offset for each player to prevent collision grouping
        player.teleport(world, spawn.x + Random.nextDouble(), spawn.y.toDouble(), spawn.z + Random.nextDouble(), player.yaw, player.pitch)
        player.setSpawnPoint(
            world.registryKey,
            spawn,
            0f, true, false,
        )
    }

    fun getTeamSpawnpoint(team: BingoTeam): BlockPos? {
        val world = getSpawnDimension()
        val spawnpoint = team.spawnpoint ?: run {
            log.warn("[SpawnService] Team ${team.name} does not have a stored spawn point! Finding a new spawn position...")

            // Get the chunk of an existing spawnpoint
            val (x, y) = state.getRegisteredTeams()
                .mapNotNull { it.spawnpoint }
                .maxByOrNull { it.x.absoluteValue }
                ?.toChunkPos()
                ?: Pair(0, 0)

            // Move "spreadPlayersDistance" away from the existing chunk on the x-axis
            val xSign = x.sign.takeIf { it != 0 } ?: 1
            val newSpawnChunk = Pair(x + (options.spawnDistance * xSign), y)

            // Store the team's new spawn position & return
            val spread = SpreadPlayers(world)
            val newSpawnPos = spread.findSpawnPos(newSpawnChunk)
            team.spawnpoint = BlockPosition.fromBlockPos(newSpawnPos)
            return newSpawnPos
        }

        val pos = spawnpoint.toBlockPos().mutableCopy()
        while (
            !isEmptyBlock(world, pos, world.getBlockState(pos))
            || !isEmptyBlock(world, pos.up(), world.getBlockState(pos.up()))
        ) {
            pos.y += 1
            if (pos.y >= world.logicalHeight) {
                log.warn("[SpawnService] Spawn position for team $team is obstructed up to build height! Finding a new spawn position...")

                val spread = SpreadPlayers(world)
                val newSpawnPos = spread.findSpawnPos(spawnpoint.toChunkPos())
                team.spawnpoint = BlockPosition.fromBlockPos(newSpawnPos)

                return newSpawnPos
            }
        }

        return pos
    }

    /**
     * Equip the player with any configured spawn equipment
     */
    fun giveSpawnEquipment(players: List<ServerPlayerEntity>) {
        if (!options.isPlayerKit) return

        val itemStacks = options.playerKitItems
            .flatMap { it.toItemStacks() }

        if (config.preventScoringSpawnKitItems) {
            for (itemStack in itemStacks) {
                // set BINGO_IGNORE flag so that spawn items do not count on the bingo card
                itemStack.getOrCreateNbt().apply {
                    putBoolean(NBT_BINGO_IGNORE, true)
                }
            }
        }

        for (stack in itemStacks) {
            for (player in players) {
                // copy the stack for each player
                val playerStack = stack.copy()
                // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
                player.giveOrEquipStack(playerStack)
            }
        }
    }

}