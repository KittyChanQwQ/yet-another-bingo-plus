package me.jfenn.bingo.spawn

import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.card.BingoTeam
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.FluidBlock
import net.minecraft.fluid.WaterFluid
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.ChunkStatus
import org.slf4j.LoggerFactory
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

class SpreadPlayers(
    private val world: ServerWorld = minecraftServer.overworld,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val invalidSpawnBiomes = setOf(
        BiomeKeys.BEACH,
        BiomeKeys.OCEAN,
        BiomeKeys.RIVER,
        BiomeKeys.COLD_OCEAN,
        BiomeKeys.DEEP_OCEAN,
        BiomeKeys.WARM_OCEAN,
        BiomeKeys.SNOWY_BEACH,
        BiomeKeys.FROZEN_RIVER,
        BiomeKeys.FROZEN_OCEAN,
        BiomeKeys.LUKEWARM_OCEAN,
        BiomeKeys.DEEP_COLD_OCEAN,
        BiomeKeys.DEEP_FROZEN_OCEAN,
        BiomeKeys.DEEP_LUKEWARM_OCEAN,
    )

    private val invalidSpawnBlocks = setOf(
        Blocks.POWDER_SNOW,
        Blocks.MAGMA_BLOCK,
        Blocks.CACTUS,
        Blocks.NETHER_WART_BLOCK,
        Blocks.WARPED_WART_BLOCK,
        Blocks.BONE_BLOCK,
        Blocks.CHORUS_PLANT,
        Blocks.CHORUS_FLOWER,
    )

    fun spreadGroups(count: Int, distance: Int): List<Pair<Int, Int>> {
        val radius = (distance * count) / (2 * Math.PI)

        val theta = 2 * Math.PI * Math.random()

        return buildList {
            for (i in 0 until count) {
                val t0 = theta + (i * 2 * Math.PI / count)
                val x = sin(t0) * radius
                val y = cos(t0) * radius

                add(Pair(x.toInt(), y.toInt()))
            }
        }
    }

    private fun isEmptyBlock(blockPos: BlockPos, blockState: BlockState): Boolean {
        return blockState.isAir || blockState.getCollisionShape(world, blockPos).isEmpty
    }

    private fun isFluidBlock(blockState: BlockState): Boolean {
        return blockState.block is FluidBlock
    }

    private fun isValidSpawn(chunk: Chunk, groundPos: BlockPos): Boolean {
        if (world.registryKey == World.OVERWORLD && groundPos.y <= world.seaLevel) {
            log.debug("Skipping block {} to avoid spawning underwater (y <= {})", groundPos, world.seaLevel)
            return false
        }

        val groundState = chunk.getBlockState(groundPos)
        if (isEmptyBlock(groundPos, groundState))
            return false
        if (isFluidBlock(groundState) || invalidSpawnBlocks.contains(groundState.block)) {
            log.debug("Skipping block {} to avoid spawning on {}", groundPos, groundState.block.translationKey)
            return false
        }

        for (y in 1..2) {
            val blockPos = BlockPos(groundPos.x, groundPos.y + y, groundPos.z)
            val blockState = chunk.getBlockState(blockPos)
            if (!isEmptyBlock(blockPos, blockState) || isFluidBlock(blockState)) {
                log.debug("Skipping block {} to avoid spawning inside {}", groundPos, blockState.block.translationKey)
                return false
            }
        }

        return true
    }

    private fun selectChunkPosition(chunk: Chunk): BlockPos? {
        // if world.dimension.hasCeiling, this needs to find a spawnpoint without using heightmaps
        // otherwise, choosing "the_nether" will spawn players on the nether roof!

        if (world.dimension.hasCeiling) {
            // choose random positions in the chunk until a valid one is found
            val spawnBlockPos = buildList {
                for (x in 2..13) {
                    for (z in 2..13) {
                        add(Pair(x, z))
                    }
                }
            }
                .shuffled()
                .asSequence()
                .map { (x, z) ->
                    chunk.pos.getBlockPos(x, world.logicalHeight - 2, z).mutableCopy()
                }
                .map { pos ->
                    // iterate until these match (false, false, true)
                    // i.e. a solid block underneath two air blocks
                    var isEmptyAbove = false
                    var isEmpty = false
                    var isEmptyBelow = false
                    while (pos.y > world.bottomY && pos.y > world.seaLevel && !(isEmptyAbove && isEmpty && !isEmptyBelow)) {
                        isEmptyAbove = isEmpty
                        isEmpty = isEmptyBelow

                        pos.move(Direction.DOWN)
                        isEmptyBelow = isEmptyBlock(pos, chunk.getBlockState(pos))
                    }

                    if (!(isEmptyAbove && isEmpty && !isEmptyBelow))
                        return@map null

                    if (!isValidSpawn(chunk, pos))
                        return@map null

                    // ensure that there is space on each side of the spawn
                    val isSpace = (1..2)
                        .flatMap {
                            listOf(
                                BlockPos(pos.x + it, pos.y, pos.z),
                                BlockPos(pos.x - it, pos.y, pos.z),
                                BlockPos(pos.x, pos.y, pos.z + it),
                                BlockPos(pos.x, pos.y, pos.z - it),
                            )
                        }
                        .all {
                            isValidSpawn(chunk, it)
                        }

                    if (!isSpace)
                        return@map null

                    pos
                }
                .find { it != null }

            return spawnBlockPos?.toImmutable()
        } else {
            // otherwise, use heightmaps to spawn players near the world surface
            val heightmapWG = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG)
            val heightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE)

            // get the highest worldgen heightmap value
            val maxHeight = (0..255)
                .maxOf {
                    heightmapWG.get(it % 16, it / 16)
                }

            // get the heightmap index closest to the chunk's worldgen height
            // this hopefully finds a reasonable actual spawn location without placing the player in a cave or on a tree
            val closestIndex = (0..255)
                .minByOrNull {
                    val height = heightmap.get(it % 16, it / 16)
                    (maxHeight - height).absoluteValue
                }
                ?: 0

            val height = heightmap.get(closestIndex % 16, closestIndex / 16)

            log.debug("chunk at (${chunk.pos.x}, ${chunk.pos.z}) height: $height")

            val spawnX = closestIndex % 16
            val spawnZ = closestIndex / 16
            val spawnBlockPos = chunk.pos.getBlockPos(spawnX, height-1, spawnZ)
            if (!isValidSpawn(chunk, spawnBlockPos))
                return null

            return spawnBlockPos
        }
    }

    fun spread(distance: Int): Map<BingoTeam, BlockPos> {
        val teams = BingoTeam.entries
            .filter { it.team().playerList.isNotEmpty() }

        val groups = spreadGroups(teams.size, distance)

        val spawns = mutableMapOf<BingoTeam, BlockPos>()

        for (i in teams.indices) {
            val team = teams[i]
            var (x, z) = groups[i]
            val xSign = x.sign.takeIf { it != 0 } ?: 1

            log.info("Spawning team ${team.name} near chunk ($x, $z)")

            findChunk@ while (true) {
                // test the rough chunk biome first, to make sure this isn't an ocean before generating the chunk
                val biomeTest = world.getBiome(BlockPos(x*16 + 8, world.logicalHeight, z*16 + 8))
                if (invalidSpawnBiomes.contains(biomeTest.key.get())) {
                    x += 4 * xSign
                    continue@findChunk
                }

                // This "!!" looks unsafe. but maybe it isn't though? (it should actually be fine)
                val chunk = world.getChunk(x, z, ChunkStatus.FULL, true)!!

                val spawnBlockPos = selectChunkPosition(chunk)
                if (spawnBlockPos == null) {
                    log.info("Skipping chunk ($x, $z) which contains no valid spawn locations")
                    x += 1 * xSign
                    continue@findChunk
                }

                if (invalidSpawnBiomes.contains(world.getBiome(spawnBlockPos).key.get())) {
                    log.info("Skipping chunk ($x, $z) for invalid spawn block biome")
                    x += 4 * xSign
                    continue@findChunk
                }

                spawns[team] = BlockPos(spawnBlockPos.x, spawnBlockPos.y + 1, spawnBlockPos.z)
                break@findChunk
            }
        }

        return spawns
    }

    fun teleportPlayer(spawnpoints: Map<BingoTeam, BlockPos>, player: ServerPlayerEntity) {
        val team = BingoTeam.fromPlayer(player)
        val spawn = team?.let {
            spawnpoints[team] ?: run {
                log.warn("Team ${team.name} does not have a stored spawnpoint")
                null
            }
        } ?: run {
            BlockPos(0, 100, 0)
        }

        player.teleport(world, spawn.x + 0.5, spawn.y + 1.0, spawn.z + 0.5, 0f, 0f)
        player.setSpawnPoint(
            world.registryKey,
            spawn,
            0f, true, false,
        )
    }

    fun teleportPlayers(spawnpoints: Map<BingoTeam, BlockPos>) {
        for (player in minecraftServer.playerManager.playerList) {
            teleportPlayer(spawnpoints, player)
        }

        // TODO: add a 1-chunk load ticket for each spawnpoint?
    }

}