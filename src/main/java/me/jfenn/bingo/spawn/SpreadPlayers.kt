package me.jfenn.bingo.spawn

import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import net.minecraft.block.Blocks
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.biome.BiomeKeys
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

    fun spread(distance: Int): Map<BingoTeam, BlockPos> {
        val teams = BingoTeam.values()
            .filter { it.team().playerList.isNotEmpty() }

        val groups = spreadGroups(teams.size, distance)

        val spawns = mutableMapOf<BingoTeam, BlockPos>()

        for (i in teams.indices) {
            val team = teams[i]
            var (x, z) = groups[i]

            log.info("Spawning team ${team.name} near chunk ($x, $z)")

            findChunk@ while (true) {
                // test the rough chunk biome first, to make sure this isn't an ocean before generating the chunk
                val biomeTest = world.getBiome(BlockPos(x*16 + 8, world.logicalHeight, z*16 + 8))
                if (invalidSpawnBiomes.contains(biomeTest.key.get())) {
                    x += 4 * x.sign
                    continue@findChunk
                }

                // This "!!" looks unsafe. but maybe it isn't though? (it should actually be fine)
                val chunk = world.getChunk(x, z, ChunkStatus.FULL, true)!!

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

                log.debug("chunk at ($x, $z) height: $height")

                val spawnX = x*16 + (closestIndex % 16)
                val spawnZ = z*16 + (closestIndex / 16)

                val spawnBlockPos = BlockPos(spawnX, height-1, spawnZ)
                val spawnBlock = world.getBlockState(spawnBlockPos)

                if (invalidSpawnBiomes.contains(world.getBiome(spawnBlockPos).key.get())) {
                    log.info("Skipping chunk ($x, $z) for invalid spawn block biome")
                    x += 4 * x.sign
                    continue@findChunk
                }

                if (spawnBlock.isLiquid || spawnBlockPos.y <= 62 || spawnBlock.block == Blocks.POWDER_SNOW) {
                    log.info("Skipping chunk ($x, $z) to avoid spawning underwater")
                    x += 1 * x.sign
                    continue@findChunk
                }

                spawns[team] = BlockPos(spawnX, height, spawnZ)
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