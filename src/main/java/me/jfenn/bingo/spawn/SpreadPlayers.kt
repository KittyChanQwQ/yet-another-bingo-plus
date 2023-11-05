package me.jfenn.bingo.spawn

import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.card.BingoTeam
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
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

    private fun selectChunkPosition(chunk: Chunk): BlockPos? {
        // if world.dimension.hasCeiling, this needs to find a spawnpoint without using heightmaps
        // otherwise, choosing "the_nether" will spawn players on the nether roof!

        if (world.dimension.hasCeiling) {
            // choose random positions in the chunk until a valid one is found
            val spawnBlockPos = buildList {
                for (x in 0..15) {
                    for (z in 0..15) {
                        add(Pair(x, z))
                    }
                }
            }
                .shuffled()
                .asSequence()
                .map { (x, z) ->
                    chunk.pos.getBlockPos(x, world.logicalHeight, z).mutableCopy()
                }
                .map { pos ->
                    // iterate until these match (false, false, true)
                    // i.e. a solid block underneath two air blocks
                    var isEmptyAbove = false
                    var isEmpty = false
                    var isEmptyBelow = false
                    var blockState: BlockState? = null
                    while (pos.y > world.bottomY && pos.y > world.seaLevel && !(isEmptyAbove && isEmpty && !isEmptyBelow)) {
                        isEmptyAbove = isEmpty
                        isEmpty = isEmptyBelow

                        blockState = chunk.getBlockState(pos.move(Direction.DOWN))
                        isEmptyBelow = blockState.let { block ->
                            block.isAir || block.getCollisionShape(world, pos).isEmpty
                        }
                    }

                    val isValidSpawn = isEmptyAbove
                            && isEmpty
                            && !isEmptyBelow
                            && blockState?.let { block -> !invalidSpawnBlocks.contains(block.block) } == true

                    if (isValidSpawn)
                        pos
                    else null
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
            return chunk.pos.getBlockPos(spawnX, height-1, spawnZ)
        }
    }

    fun spread(distance: Int): Map<BingoTeam, BlockPos> {
        val teams = BingoTeam.values()
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

                val spawnBlockBelow = world.getBlockState(spawnBlockPos)

                if (invalidSpawnBiomes.contains(world.getBiome(spawnBlockPos).key.get())) {
                    log.info("Skipping chunk ($x, $z) for invalid spawn block biome")
                    x += 4 * xSign
                    continue@findChunk
                }

                if (spawnBlockBelow.isLiquid || (world.registryKey == World.OVERWORLD && spawnBlockPos.y <= world.seaLevel)) {
                    log.info("Skipping chunk ($x, $z) to avoid spawning underwater (y <= ${world.seaLevel})")
                    x += 1 * xSign
                    continue@findChunk
                }

                if (invalidSpawnBlocks.contains(spawnBlockBelow.block)) {
                    log.info("Skipping chunk ($x, $z) to avoid spawning on ${spawnBlockBelow.block.name}")
                    x += 1 * xSign
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