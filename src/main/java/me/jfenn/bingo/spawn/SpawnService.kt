package me.jfenn.bingo.spawn

import me.jfenn.bingo.NBT_BINGO_IGNORE
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BlockPosition
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.utils.giveOrEquipStack
import net.minecraft.block.Blocks
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
import org.slf4j.LoggerFactory
import kotlin.random.Random

object SpawnService {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun getSpawnDimension(ctx: BingoContext): ServerWorld {
        return ctx.server.worlds.find {
            it.registryKey.value.toString() == ctx.config.spawnDimension
        } ?: run {
            log.error("Could not find spawnDimension '${ctx.config.spawnDimension}'; defaulting to minecraft:overworld")
            ctx.server.overworld
        }
    }

    /**
     * Create team spawnpoints and teleport teams to their spawns
     */
    fun createSpawnpoints(ctx: BingoContext) {
        val world = getSpawnDimension(ctx)

        // spread player teams to spawnpoint
        val spread = SpreadPlayers(world)
        ctx.state.teamSpawnpoints = spread.spread(ctx.config.spreadPlayersDistance)
            .mapValues { (_, value) -> BlockPosition.fromBlockPos(value) }

        ctx.state.teamSpawnpoints.forEach { (_, blockPos) ->
            createTeamChest(ctx.config, world, blockPos)
        }
    }

    fun teleportPlayer(ctx: BingoContext, player: ServerPlayerEntity) {
        val team = BingoTeam.fromPlayer(player)
        val spawn = team?.let {
            getTeamSpawnpoint(ctx, team)
        } ?: run {
            BlockPos(0, 100, 0)
        }

        // random offset for each player to prevent collision grouping
        val world = getSpawnDimension(ctx)
        player.teleport(world, spawn.x + Random.nextDouble(), spawn.y.toDouble(), spawn.z + Random.nextDouble(), 0f, 0f)
        player.setSpawnPoint(
            world.registryKey,
            spawn,
            0f, true, false,
        )
    }

    fun getTeamSpawnpoint(ctx: BingoContext, team: BingoTeam): BlockPos? {
        val spawnpoint = ctx.state.teamSpawnpoints[team] ?: run {
            log.warn("Team ${team.name} does not have a stored spawnpoint")
            return null
        }
        val world = getSpawnDimension(ctx)

        val pos = spawnpoint.toBlockPos().mutableCopy()
        while (
            !isEmptyBlock(world, pos, world.getBlockState(pos))
            || !isEmptyBlock(world, pos.up(), world.getBlockState(pos.up()))
        ) {
            pos.y += 1
            if (pos.y >= world.logicalHeight) {
                log.error("Spawn position for team $team is obstructed up to build height...")
                return spawnpoint.toBlockPos()
            }
        }

        return pos
    }

    /**
     * Place a team kit chest in the world, nearby the provided [spawnpoint]
     * Similar behavior to the BonusChestFeature
     */
    private fun createTeamChest(config: BingoConfig, world: ServerWorld, spawnpoint: BlockPosition) {
        if (!config.isTeamKit) return

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
        config.teamKitItems
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
    fun giveSpawnEquipment(config: BingoConfig, players: List<ServerPlayerEntity>) {
        if (!config.isPlayerKit) return

        val itemStacks = config.playerKitItems
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