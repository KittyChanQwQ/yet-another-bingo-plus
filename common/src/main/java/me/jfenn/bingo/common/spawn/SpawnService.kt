package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.giveOrEquipStack
import net.minecraft.block.Blocks
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
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
            log.error("Could not find spawnDimension '${options.spawnDimension}'; defaulting to minecraft:overworld")
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

        for (team in state.getRegisteredTeams()) {
            team.spawnpoint?.let {
                createTeamChest(world, it)
            }
        }
    }

    fun teleportPlayer(player: ServerPlayerEntity) {
        val world = getSpawnDimension()

        val team = teamService.getPlayerTeam(player)
        val spawn = team?.let {
            getTeamSpawnpoint(team)
        } ?: run {
            world.spawnPos
        }

        // random offset for each player to prevent collision grouping
        player.teleport(world, spawn.x + Random.nextDouble(), spawn.y.toDouble(), spawn.z + Random.nextDouble(), 0f, 0f)
        player.setSpawnPoint(
            world.registryKey,
            spawn,
            0f, true, false,
        )
    }

    fun getTeamSpawnpoint(team: BingoTeam): BlockPos? {
        val world = getSpawnDimension()
        val spawnpoint = team.spawnpoint ?: run {
            log.warn("Team ${team.name} does not have a stored spawn point!")
            log.warn("Finding a new spawn position...")

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
                log.warn("Spawn position for team $team is obstructed up to build height!")
                log.warn("Finding a new spawn position...")

                val spread = SpreadPlayers(world)
                val newSpawnPos = spread.findSpawnPos(spawnpoint.toChunkPos())
                team.spawnpoint = BlockPosition.fromBlockPos(newSpawnPos)

                return newSpawnPos
            }
        }

        return pos
    }

    /**
     * Place a team kit chest in the world, nearby the provided [spawnpoint]
     * Similar behavior to the BonusChestFeature
     */
    private fun createTeamChest(world: ServerWorld, spawnpoint: BlockPosition) {
        if (!options.isTeamKit) return

        val chunk = world.getChunk(spawnpoint.toBlockPos())
        // if the dimension has a ceiling, we can't use heightmaps...
        val chestBlockPos: BlockPos? = if (world.dimension.hasCeiling) {
            // spawn the chest directly next to the player
            BlockPos(spawnpoint.x + 1, spawnpoint.y, spawnpoint.z)
        } else {
            val heightmap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES)

            // Choose a random position in the chunk to spawn the team chest at
            buildList {
                for (x in 0..15) {
                    for (z in 0..15) {
                        add(Pair(x, z))
                    }
                }
            }
                .shuffled()
                .asSequence()
                .map { (x, z) ->
                    val y = heightmap.get(x, z)
                    chunk.pos.getBlockPos(x, y, z)
                }
                .find {  blockPos ->
                    world.isAir(blockPos) || world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty
                }
        }

        if (chestBlockPos == null) {
            log.error("No valid locations to create a team kit chest!")
            return
        }

        // Create the team chest block/entity at the chosen position
        world.setBlockState(chestBlockPos, Blocks.CHEST.defaultState)
        val entity = world.getBlockEntity(chestBlockPos) as? ChestBlockEntity ?: run {
            log.error("Creating a team kit chest did not create a block entity. This should never happen!!")
            return
        }

        // Place torches around the chest in each direction
        val torchState = Blocks.TORCH.defaultState
        Direction.Type.HORIZONTAL
            .map { direction -> chestBlockPos.offset(direction) }
            .filter { blockPos -> torchState.canPlaceAt(world, blockPos) }
            .forEach { blockPos -> world.setBlockState(blockPos, torchState) }

        // Fill the chest with the configured item stacks
        options.teamKitItems
            .flatMap { it.toItemStacks() }
            .forEachIndexed { i, itemStack ->
                if (config.preventScoringSpawnKitItems) {
                    // set BINGO_IGNORE flag so that spawn items do not count on the bingo card
                    itemStack.getOrCreateNbt().apply {
                        putBoolean(NBT_BINGO_IGNORE, true)
                    }
                }

                entity.setStack(i, itemStack)
            }
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