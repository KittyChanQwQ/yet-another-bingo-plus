package me.jfenn.bingo.common.map

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.LruHashMap
import net.minecraft.item.FilledMapItem
import net.minecraft.item.Item
import net.minecraft.item.map.MapState
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class MapRenderService(
    private val log: Logger,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
) {

    companion object {
        private val COLOR_BACKGROUND = Color(130, 130, 130)
        private val COLOR_ITEM_SHADOW = Color(100, 100, 100)
        private val COLOR_MARGIN = Color(199, 199, 199)
        private val COLOR_SHADOW = Color(20, 20, 20)
        private val COLOR_ACHIEVED = Color(100, 255, 100)
        private val COLOR_FLASHING = Color(178, 76, 216)
        private val COLOR_ACHIEVED_SHADOW = Color(80, 120, 80)
        private val COLOR_LOCKED = Color(150, 0, 0)

        private const val MAP_SIZE = 128
        private const val MAP_BORDER = 5

        private const val TILE_IMAGE = 16
        private const val TILE_PADDING = 3
        private const val TILE_MARGIN = 2
        private const val TILE_SIZE = TILE_IMAGE + TILE_PADDING*2 + TILE_MARGIN
        private const val IMAGE_SIZE = TILE_IMAGE + TILE_PADDING*2

        private val FLASHING_DURATION = 4.seconds

        fun validateItems() {
            for (item in Registries.ITEM.keys) {
                try {
                    readTileImage(item.value).close()
                } catch (e: Throwable) {
                    val log = LoggerFactory.getLogger(MOD_ID)
                    log.warn("Missing item image: ${item.value.path}.png")
                }
            }
        }

        private fun readTileImage(item: Identifier): InputStream {
            return Config.readStream("images/${item.path}.png", false) {
                MapRenderService::class.java.getResourceAsStream("/bingo/images/${item.path}.png")
                    ?: throw IllegalArgumentException("Cannot find default texture for ${item.path}.png")
            }
        }
    }

    init {
        validateItems()
    }

    class BingoTileImage(
        val regular: ByteArray = ByteArray(IMAGE_SIZE*IMAGE_SIZE).apply { fill(COLOR_BACKGROUND.asByte) },
        val flashing: ByteArray = ByteArray(IMAGE_SIZE*IMAGE_SIZE).apply { fill(COLOR_FLASHING.asByte) },
        val achieved: ByteArray = ByteArray(IMAGE_SIZE*IMAGE_SIZE).apply { fill(COLOR_ACHIEVED.asByte) },
    ) {
        companion object {
            val EMPTY = BingoTileImage()
        }
    }

    // keep a full card of 25 items in-memory, using LRU eviction
    private val imageMap = LruHashMap<Item, BingoTileImage?>(25)

    private fun getTileImage(entry: BingoCardEntry): BingoTileImage? {
        val item = entry.visualItem ?: return null
        if (imageMap.containsKey(item))
            return imageMap[item]

        try {
            val identifier = Registries.ITEM.getId(item)
            val image = readTileImage(identifier).use { ImageIO.read(it) }
            val tileImage = BingoTileImage()

            for (x in 0 until IMAGE_SIZE) {
                for (y in 0 until IMAGE_SIZE) {
                    val i = x + y * IMAGE_SIZE

                    val imgX = x - TILE_PADDING
                    val imgY = y - TILE_PADDING

                    var color = if (imgX in 0 until TILE_IMAGE && imgY in 0 until TILE_IMAGE) {
                        Color.fromInt(image.getRGB(imgX, imgY))
                    } else Color.fromInt(0)

                    val prevColor = if (imgX-1 in 0 until TILE_IMAGE && imgY-1 in 0 until TILE_IMAGE) {
                        Color.fromInt(image.getRGB(imgX-1, imgY-1))
                    } else Color.fromInt(0)

                    if (color.a == 0) {
                        if (prevColor.a != 0) {
                            tileImage.regular[i] = COLOR_ITEM_SHADOW.asByte
                            tileImage.flashing[i] = COLOR_ITEM_SHADOW.asByte
                            tileImage.achieved[i] = COLOR_ACHIEVED_SHADOW.asByte
                        }
                        continue
                    }

                    // if the image is transparent (glass), blend with the card background
                    if (color.a < 255) color = COLOR_BACKGROUND.mix(color.copy(a = (color.a * 2).coerceAtMost(255)))

                    tileImage.regular[i] = color.asByte
                    tileImage.flashing[i] = color.asByte
                    // make the item image slightly transparent when achieved
                    tileImage.achieved[i] = COLOR_ACHIEVED.mix(color.copy(a = 200)).asByte
                }
            }

            imageMap[item] = tileImage
            return tileImage
        } catch (e: Throwable) {
            log.error("Error reading item image: ${item.translationKey}", e)
            imageMap[item] = null
            return null
        }
    }

    fun getMapColor(team: BingoTeam?): Byte {
        return team?.mapColor ?: 22
    }

    private fun drawMapBorders(
        mapState: MapState,
        team: BingoTeam?,
    ) {
        val borderLight = (getMapColor(team)*4 + 2).toUByte().toByte()
        val borderMid = (getMapColor(team)*4 + 1).toUByte().toByte()
        val borderDark = (getMapColor(team)*4 + 3).toUByte().toByte()

        // Outer edge borders
        for (x in 0 until MAP_SIZE) {
            mapState.setColor(x, 0, borderLight)
            mapState.setColor(0, x, borderLight)

            mapState.setColor(x, MAP_SIZE-1, borderDark)
            mapState.setColor(MAP_SIZE-1, x, borderDark)
        }

        // Middle borders
        for (x in 1 until MAP_SIZE-1) {
            for (y in 1 until MAP_BORDER-1) {
                val yInv = MAP_SIZE - y - 1
                mapState.setColor(x, y, borderMid)
                mapState.setColor(y, x, borderMid)
                mapState.setColor(x, yInv, borderMid)
                mapState.setColor(yInv, x, borderMid)
            }
        }

        // Inner borders
        for (x in MAP_BORDER-1..MAP_SIZE-MAP_BORDER) {
            mapState.setColor(x, MAP_BORDER-1, borderDark)
            mapState.setColor(MAP_BORDER-1, x, borderDark)
            mapState.setColor(x, MAP_SIZE-MAP_BORDER, borderLight)
            mapState.setColor(MAP_SIZE-MAP_BORDER, x, borderLight)
        }

        // Cell borders
        for (x in MAP_BORDER until MAP_SIZE-MAP_BORDER) {
            for (y in 1..4) {
                val yActual = MAP_BORDER + y*TILE_SIZE - TILE_MARGIN + 1
                mapState.setColor(x, yActual, COLOR_SHADOW.asByte)
                mapState.setColor(yActual, x, COLOR_SHADOW.asByte)
            }
        }
        for (x in MAP_BORDER until MAP_SIZE-MAP_BORDER) {
            for (y in 1..4) {
                val yActual = MAP_BORDER + y*TILE_SIZE - TILE_MARGIN
                mapState.setColor(x, yActual, COLOR_MARGIN.asByte)
                mapState.setColor(yActual, x, COLOR_MARGIN.asByte)
            }
        }
    }

    private fun drawMapTile(
        team: BingoTeam?,
        mapState: MapState,
        item: BingoCardEntry,
        itemX: Int,
        itemY: Int,
    ) {
        require(itemX in 0 until 5)
        require(itemY in 0 until 5)

        val isAchieved = team != null && item.hasAchieved(team)
        val isFlashing = item.isFlashing(team, FLASHING_DURATION)
                && (server.ticks / 10) % 2 == 0

        val isLocked = options.isLockoutMode && item.teams.isNotEmpty() && team != null && !item.hasAchieved(team)
        val tileImage = getTileImage(item) ?: BingoTileImage.EMPTY
        val tileImageArray = when {
            options.hiddenItems && (team == null || !item.hasSeen(team)) -> BingoTileImage.EMPTY.regular
            isFlashing -> tileImage.flashing
            isAchieved -> tileImage.achieved
            else -> tileImage.regular
        }

        val itemTeams = item.teams.mapNotNull { state.teams[it] }
            .filter { it.key != team?.key }

        val tileX = MAP_BORDER + TILE_SIZE*itemX
        val tileY = MAP_BORDER + TILE_SIZE*itemY

        val innerMax = TILE_SIZE - TILE_MARGIN - 1
        for (innerY in 0..innerMax) {
            val y = tileY + innerY
            val mapStateStart = tileX + y * MAP_SIZE
            val tileImageStart = innerY * IMAGE_SIZE
            // Uses System.arraycopy, which is a lot faster than manually setting each byte
            tileImageArray.copyInto(mapState.colors, mapStateStart, tileImageStart, tileImageStart + IMAGE_SIZE)
        }

        if (isLocked) {
            for (innerT in 0..innerMax) {
                val x = tileX + innerT
                val y = tileY + innerT
                mapState.setColor(x, y, COLOR_LOCKED.asByte)

                val yInv = tileY + innerMax - innerT
                mapState.setColor(x, yInv, COLOR_LOCKED.asByte)

                if (innerT > 0) {
                    mapState.setColor(x-1, y, COLOR_LOCKED.asByte)
                    mapState.setColor(x-1, yInv, COLOR_LOCKED.asByte)
                }
            }
        }

        if (
            options.showOtherTeamCompletions &&
            item.hasAnyAchieved()
        ) {
            for (innerT in 0..innerMax*2 + 1) {
                val tileTeamX = innerT * itemTeams.size / (innerMax*2 + 1)
                val itemTeam = itemTeams.getOrNull(tileTeamX) ?: continue
                val variant = if (innerT % 2 == 0) 2 else 3
                val color = (getMapColor(itemTeam)*4 + variant).toUByte().toByte()

                if (innerT < innerMax) {
                    mapState.setColor(tileX + innerT, tileY, color)
                    mapState.setColor(tileX, tileY + innerT, color)
                } else {
                    mapState.setColor(tileX + innerT - innerMax, tileY + innerMax, color)
                    mapState.setColor(tileX + innerMax, tileY + innerT - innerMax, color)
                }
            }
        }

        for (x in tileX..tileX + innerMax) {
            for (y in tileY..tileY + innerMax) {
                // mark every pixel in the tile as dirty
                mapState.markDirty(x, y)
            }
        }
    }

    fun update(
        team: BingoTeam?,
    ) = measureTime {
        val card = state.card
        val map = team?.getMap(server) ?: state.getPreviewMap(server)

        val state = map.mapState ?: kotlin.run {
            val state = MapState.of(1, true, server.overworld.registryKey)
            drawMapBorders(state, team)

            server.overworld.putMapState(FilledMapItem.getMapName(map.mapId), state)
            map.mapState = state
            state
        }

        for (x in 0 until 5) {
            for (y in 0 until 5) {
                val entry = card.entry(x, y)
                if (
                    entry.updatedAt(team) > map.updatedAt ||
                    (options.showOtherTeamCompletions && entry.updatedAt > map.updatedAt) ||
                    (entry.isFlashing(team, FLASHING_DURATION + 1.seconds) && server.ticks % 10 == 0)
                ) {
                    drawMapTile(
                        mapState = state,
                        team = team,
                        item = entry,
                        itemX = x,
                        itemY = y,
                    )
                }
            }
        }

        map.updatedAt = Clock.System.now()
    }.also {
        if (it >= 1.milliseconds) {
            log.info("Re-drawing map_${team?.map?.mapId} for team ${team?.name} ($it)")
        }
    }

}