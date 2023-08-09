package me.jfenn.bingo.spawn

import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.chunk.ChunkStatus
import org.slf4j.LoggerFactory
import kotlin.math.*

object SpreadPlayers {

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

    fun spreadGroups(count: Int): List<Pair<Int, Int>> {
        val distance = 1000
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

    fun spread(): Map<BingoTeam, BlockPos> {
        val teams = BingoTeam.values()
            .filter { it.team().playerList.isNotEmpty() }

        val groups = spreadGroups(teams.size)

        val spawns = mutableMapOf<BingoTeam, BlockPos>()

        for (i in teams.indices) {
            val team = teams[i]
            var (x, z) = groups[i]

            log.info("Spawning team ${team.name} near chunk ($x, $z)")

            findChunk@ while (true) {
                // This looks unsafe. but maybe it isn't though?
                val chunk = minecraftServer.overworld.getChunk(x, z, ChunkStatus.FULL, true)!!

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
                val spawnBlock = minecraftServer.overworld.getBlockState(spawnBlockPos)


                if (invalidSpawnBiomes.contains(minecraftServer.overworld.getBiome(spawnBlockPos).key.get())) {
                    log.info("Skipping chunk ($x, $z) for invalid biome")
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
        val team = BingoTeam.fromPlayer(player) ?: return
        val spawn = spawnpoints[team] ?: run {
            log.warn("Team ${team.name} does not have a stored spawnpoint")
            return
        }

        player.teleport(minecraftServer.overworld, spawn.x + 0.5, spawn.y + 1.0, spawn.z + 0.5, 0f, 0f)
        player.setSpawnPoint(
            minecraftServer.overworld.registryKey,
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