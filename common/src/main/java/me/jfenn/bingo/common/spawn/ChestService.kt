package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.IServerWorldFactory
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.IChestBlockState
import me.jfenn.bingo.platform.block.ITorchBlockState
import me.jfenn.bingo.platform.world.IChestBlockEntity
import me.jfenn.bingo.platform.world.IChunkHeightmap
import org.slf4j.Logger
import kotlin.random.Random

internal class ChestService(
    private val log: Logger,
    private val serverWorldFactory: IServerWorldFactory,
    private val options: BingoOptions,
    private val config: BingoConfig,
    private val state: BingoState,
    private val spawnKitService: SpawnKitService,
) {

    private fun getSpawnDimension(): IServerWorld {
        return serverWorldFactory.listWorlds().find {
            it.identifier == options.spawnDimension
        } ?: run {
            log.error("[SpawnService] Could not find spawnDimension '${options.spawnDimension}'; defaulting to minecraft:overworld")
            serverWorldFactory.overworld
        }
    }

    fun createChestSpawnpoints() {
        if (!options.isTeamKit) return

        for (team in state.getRegisteredTeams()) {
            if (team.chestSpawnpoint == null) {
                createChestSpawnpoint(team, state.randomSeed())
            }
        }
    }

    private fun createChestSpawnpoint(team: BingoTeam, seed: Long): BlockPosition? {
        if (!options.isTeamKit) return null
        if (!state.isLobbyMode) return null
        val world = getSpawnDimension()
        val spawnpoint = team.spawnpoint ?: run {
            log.error("[ChestService] createChestSpawnpoint - Cannot create a team chest, as the team does not have a spawn point!")
            return null
        }

        val chunk = world.getChunk(spawnpoint)
        // if the dimension has a ceiling, we can't use heightmaps...
        val chestBlockPos: BlockPosition? = if (world.hasCeiling) {
            // spawn the chest directly next to the player
            BlockPosition(spawnpoint.x + 1, spawnpoint.y, spawnpoint.z)
        } else {
            val heightmap = chunk.getHeightmap(IChunkHeightmap.Type.MOTION_BLOCKING_NO_LEAVES)

            // Choose a random position in the chunk to spawn the team chest at
            buildList {
                for (x in 0..15) {
                    for (z in 0..15) {
                        add(Pair(x, z))
                    }
                }
            }
                .shuffled(Random(seed))
                .asSequence()
                .map { (x, z) ->
                    val y = heightmap.get(x, z)
                    chunk.pos.getBlockPos(x, y, z)
                }
                .find {  blockPos ->
                    world.isAir(blockPos) || world.isEmptyCollision(blockPos)
                }
        }

        if (chestBlockPos == null) {
            log.error("[ChestService] createChestSpawnpoint - No valid locations to create a team kit chest!")
            return null
        }

        team.chestSpawnpoint = chestBlockPos
        return chestBlockPos
    }

    fun createChestBlock(team: BingoTeam) {
        if (!options.isTeamKit) return
        if (!state.isLobbyMode) return
        val world = getSpawnDimension()
        val chestSpawnpoint = team.chestSpawnpoint ?: createChestSpawnpoint(team, state.randomSeed()) ?: run {
            log.error("[ChestService] createChestBlock - Cannot create a team chest, as there is no chestSpawnpoint.")
            return
        }

        // Create the team chest block/entity at the chosen position
        world.setBlockState(chestSpawnpoint, world.createBlockState(IChestBlockState.Type))
        val entity = world.getBlockEntity(chestSpawnpoint) as? IChestBlockEntity ?: run {
            log.error("[SpawnService] createTeamChest - Creating a team kit chest did not create a block entity. This should never happen!!")
            return
        }

        // Place torches around the chest in each direction
        val torchState = world.createBlockState(ITorchBlockState.Type)
        listOf(
            chestSpawnpoint.move(1, 0, 0),
            chestSpawnpoint.move(-1, 0, 0),
            chestSpawnpoint.move(0, 0, 1),
            chestSpawnpoint.move(0, 0, -1)
        )
            .filter { blockPos -> torchState.canSurvive(world, blockPos) }
            .forEach { blockPos -> world.setBlockState(blockPos, torchState) }

        // Fill the chest with the configured item stacks
        spawnKitService.getTeamItems()
            .forEachIndexed { i, itemStack ->
                if (config.preventScoringSpawnKitItems) {
                    // set BINGO_IGNORE flag so that spawn items do not count on the bingo card
                    itemStack.addCustomTag(NBT_BINGO_IGNORE)
                }

                entity.inventory[i] = itemStack
            }
    }

}