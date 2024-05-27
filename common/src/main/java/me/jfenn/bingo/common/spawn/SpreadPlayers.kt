package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.config.BlockPosition
import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.ChunkStatus
import org.slf4j.LoggerFactory
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.system.measureTimeMillis

class SpreadPlayers(
    private val world: ServerWorld,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    // Clamp spawn positions to the overworld border
    // - yes, even if the spawn dimension isn't in the overworld...
    private val worldBorder = world.server.overworld.worldBorder
    private val coordinateScale = world.dimension.coordinateScale
    private val worldBorderCenterInWorld = BlockPos((worldBorder.centerX / coordinateScale).toInt(), 0, (worldBorder.centerZ / coordinateScale).toInt())

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
        val center = ChunkPos(worldBorderCenterInWorld)
        val radius = ((distance * count) / (2 * Math.PI))
            .coerceAtMost(ChunkSectionPos.getSectionCoord(worldBorder.maxRadius).toDouble() - 16.0)
            .coerceAtLeast(0.0)

        val theta = 2 * Math.PI * Math.random()

        return buildList {
            for (i in 0 until count) {
                val t0 = theta + (i * 2 * Math.PI / count)
                val x = sin(t0) * radius
                val z = cos(t0) * radius

                add(Pair(center.x + x.toInt(), center.z + z.toInt()))
            }
        }
    }

    private fun isValidSpawn(chunk: Chunk, groundPos: BlockPos): Boolean {
        // scale the groundPos to match overworld positions for the worldBorder
        val groundPosInOverworld = BlockPos(
            groundPos.x.times(coordinateScale).toInt(),
            groundPos.y,
            groundPos.z.times(coordinateScale).toInt(),
        )
        if (!worldBorder.contains(groundPosInOverworld)) {
            log.debug("Skipping block {} to avoid spawning outside the world border", groundPos)
            return false
        }

        if (world.registryKey == World.OVERWORLD && groundPos.y <= world.seaLevel) {
            log.debug("Skipping block {} to avoid spawning underwater (y <= {})", groundPos, world.seaLevel)
            return false
        }

        val groundState = chunk.getBlockState(groundPos)
        if (isEmptyBlock(world, groundPos, groundState))
            return false
        if (isFluidBlock(groundState) || invalidSpawnBlocks.contains(groundState.block)) {
            log.debug("Skipping block {} to avoid spawning on {}", groundPos, groundState.block.translationKey)
            return false
        }

        for (y in 1..2) {
            val blockPos = BlockPos(groundPos.x, groundPos.y + y, groundPos.z)
            val blockState = chunk.getBlockState(blockPos)
            if (!isEmptyBlock(world, blockPos, blockState) || isFluidBlock(blockState)) {
                log.debug("Skipping block {} to avoid spawning inside {}", groundPos, blockState.block.translationKey)
                return false
            }
        }

        return true
    }
    private fun everyChunkIndex(margin: Int) = sequence {
        for (x in margin until 16 - margin) {
            for (z in margin until 16 - margin) {
                yield(x + z * 16)
            }
        }
    }

    private fun adjacentBlocks(pos: BlockPos, range: Int) = sequence {
        for (x in -range..range) {
            for (z in -range..range) {
                if (x == 0 && z == 0) continue
                yield(BlockPos(pos.x + x, pos.y, pos.z + z))
            }
        }
    }

    private fun selectChunkPosition(chunk: Chunk): BlockPos? {
        // if world.dimension.hasCeiling, this needs to find a spawnpoint without using heightmaps
        // otherwise, choosing "the_nether" will spawn players on the nether roof!

        if (world.dimension.hasCeiling) {
            // choose random positions in the chunk until a valid one is found
            val spawnBlockPos = everyChunkIndex(2)
                .shuffled()
                .map {
                    chunk.pos.getBlockPos(it % 16, world.logicalHeight - 2, it / 16).mutableCopy()
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
                        isEmptyBelow = isEmptyBlock(world, pos, chunk.getBlockState(pos))
                    }

                    if (!(isEmptyAbove && isEmpty && !isEmptyBelow))
                        return@map null

                    if (!isValidSpawn(chunk, pos))
                        return@map null

                    // ensure that there is space on each side of the spawn
                    val isSpace = adjacentBlocks(pos, 2)
                        .all { isValidSpawn(chunk, it) }

                    if (!isSpace)
                        return@map null

                    pos
                }
                .find { it != null }

            return spawnBlockPos?.toImmutable()
        } else {
            // otherwise, use heightmaps to spawn players near the world surface
            // this hopefully finds a reasonable actual spawn location without placing the player in a cave or on a tree
            val heightmap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES)

            return everyChunkIndex(1)
                .shuffled()
                .map {
                    val x = it % 16
                    val z = it / 16
                    val height = heightmap.get(x, z)
                    chunk.pos.getBlockPos(x, height - 1, z)
                }
                .filter {
                    // if the block is below sea level, skip it!
                    !(world.registryKey == World.OVERWORLD && it.y <= world.seaLevel)
                }
                .find { pos ->
                    isValidSpawn(chunk, pos) && adjacentBlocks(pos, 1).all { isValidSpawn(chunk, it) }
                }
        }
    }

    /**
     * Searches for a valid spawn position, starting at the provided chunk and iterating
     * outwards on the X axis.
     */
    fun findSpawnPos(startChunk: Pair<Int, Int>): BlockPos {
        var (x, z) = startChunk
        val xSign = x.sign.takeIf { it != 0 } ?: 1

        while (true) {
            // test the rough chunk biome first, to make sure this isn't an ocean before generating the chunk
            val chunkCenter = BlockPos(x*16 + 8, world.logicalHeight, z*16 + 8)
            val biomeTest = world.getBiome(chunkCenter)
            if (invalidSpawnBiomes.contains(biomeTest.key.get())) {
                x += 4 * xSign
                continue
            }

            // Scale the chunkCenter to match overworld positions for the worldBorder
            val chunkCenterInOverworld = BlockPos(
                chunkCenter.x.times(coordinateScale).toInt(),
                chunkCenter.y,
                chunkCenter.z.times(coordinateScale).toInt(),
            )
            if (!worldBorder.contains(chunkCenterInOverworld)) {
                log.warn("Selected chunk is outside of the world border - attempting to find any valid position...")
                val centerChunkPos = ChunkPos(worldBorderCenterInWorld)
                val centerChunk = world.getChunk(centerChunkPos.x, centerChunkPos.z)
                selectChunkPosition(centerChunk)
                    ?.let { return it }

                log.error("Could not find any valid spawn location due to the world border. Using the default world spawn, which may be buggy...")
                return world.spawnPos
            }

            // This "!!" looks unsafe. but maybe it isn't though? (it should actually be fine)
            val chunk = world.getChunk(x, z, ChunkStatus.FULL, true)!!

            val spawnBlockPos = selectChunkPosition(chunk)
            if (spawnBlockPos == null) {
                log.info("Skipping chunk ($x, $z) which contains no valid spawn locations")
                x += 1 * xSign
                continue
            }

            if (invalidSpawnBiomes.contains(world.getBiome(spawnBlockPos).key.get())) {
                log.info("Skipping chunk ($x, $z) for invalid spawn block biome")
                x += 4 * xSign
                continue
            }

            return BlockPos(spawnBlockPos.x, spawnBlockPos.y + 1, spawnBlockPos.z)
        }
    }

    fun spread(
        teams: List<BingoTeam>,
        distance: Int
    ) {
        val groups = spreadGroups(teams.size, distance)

        for (i in teams.indices) {
            val team = teams[i]
            val startChunk = groups[i]

            log.info("Spawning team ${team.id} near chunk $startChunk")

            val chunkLocatorMs = measureTimeMillis {
                team.spawnpoint = BlockPosition.fromBlockPos(
                    findSpawnPos(startChunk)
                )
            }

            log.info("Found a spawn location for ${team.id} in ${chunkLocatorMs}ms")
        }
    }

}