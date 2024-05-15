package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.config.TierLabel
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.LruHashMap
import net.minecraft.item.map.MapState
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Formatting
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class MapRenderService(
    private val log: Logger,
    private val server: MinecraftServer,
    private val mapService: IMapService,
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
        private val COLOR_WHITE = Color(255, 255, 255)

        private const val MAP_SIZE = 128
        private const val MAP_BORDER = 5

        private const val TILE_IMAGE = 16
        private const val TILE_PADDING = 3
        private const val TILE_MARGIN = 2
        private const val TILE_SIZE = TILE_IMAGE + TILE_PADDING*2 + TILE_MARGIN
        private const val IMAGE_SIZE = TILE_IMAGE + TILE_PADDING*2

        fun validateItems() {
            for (item in Registries.ITEM.keys) {
                try {
                    readTileImage(item.value.path).close()
                } catch (e: Throwable) {
                    val log = LoggerFactory.getLogger(MOD_ID)
                    log.warn("Missing item image: ${item.value.path}.png")
                }
            }
        }

        private fun readTileImage(itemPath: String): InputStream {
            return Config.readStream("images/${itemPath}.png", false) {
                MapRenderService::class.java.getResourceAsStream("/bingo/images/${itemPath}.png")
                    ?: throw IllegalArgumentException("Cannot find default texture for ${itemPath}.png")
            }
        }

        val IMAGE_FRAME_ADVANCEMENT: ByteArray = run {
            val image = MapRenderService::class.java.getResourceAsStream("/bingo/image_frame_advancement.png")
                .use { ImageIO.read(it) }

            val arr = ByteArray(IMAGE_SIZE*IMAGE_SIZE)
            for (x in 0 until IMAGE_SIZE) for (y in 0 until IMAGE_SIZE) {
                val color = Color.fromInt(image.getRGB(x, y))
                if (color.a != 0) {
                    arr[x + y * IMAGE_SIZE] = color.asByte
                }
            }

            arr
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

            val FALLBACK = MapRenderService::class.java.getResourceAsStream("/bingo/image_fallback.png")
                    .use { ImageIO.read(it) }
                    .let { fromBufferedImage(it) }

            val HIDDEN = MapRenderService::class.java.getResourceAsStream("/bingo/image_hidden.png")
                .use { ImageIO.read(it) }
                .let { fromBufferedImage(it) }

            val HIDDEN_TIERS = TierLabel.entries.associateWith { tier ->
                MapRenderService::class.java.getResourceAsStream("/bingo/image_hidden_${tier.name.lowercase()}.png")
                    .use { ImageIO.read(it) }
                    .let { fromBufferedImage(it) }
            }

            fun fromBufferedImage(image: BufferedImage): BingoTileImage {
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

                return tileImage
            }
        }
    }

    // keep a full card of 25 items in-memory, using LRU eviction
    // (could need extra space to handle rotating items)
    private val imageMap = LruHashMap<String, BingoTileImage?>(25)

    private fun getTileImage(tile: CardTile): BingoTileImage? {
        val item = tile.item
            ?.item
            ?.let { Registries.ITEM.getId(it).path }
            ?: return null

        if (imageMap.containsKey(item))
            return imageMap[item] ?: BingoTileImage.FALLBACK

        try {
            val image = readTileImage(item).use { ImageIO.read(it) }
            val tileImage = BingoTileImage.fromBufferedImage(image)

            imageMap[item] = tileImage
            return tileImage
        } catch (e: Throwable) {
            log.error("Error reading item image: $item.png", e)
            imageMap[item] = null
            return BingoTileImage.FALLBACK
        }
    }

    fun getMapColor(color: Formatting?): Byte {
        return when (color) {
            Formatting.BLUE -> 12
            Formatting.RED -> 4
            Formatting.GREEN -> 33
            Formatting.YELLOW -> 30
            Formatting.LIGHT_PURPLE -> 20
            Formatting.AQUA -> 31
            Formatting.GOLD -> 15
            Formatting.GRAY -> 21
            else -> 22
        }
    }

    private fun drawMapBorders(
        mapState: MapState,
        team: BingoTeam?,
    ) {
        val borderLight = (getMapColor(team?.formatting)*4 + 2).toUByte().toByte()
        val borderMid = (getMapColor(team?.formatting)*4 + 1).toUByte().toByte()
        val borderDark = (getMapColor(team?.formatting)*4 + 3).toUByte().toByte()

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
        mapState: MapState,
        tile: CardTile,
        itemX: Int,
        itemY: Int,
    ) {
        require(itemX in 0 until 5)
        require(itemY in 0 until 5)

        val tileImage = getTileImage(tile) ?: BingoTileImage.EMPTY
        val tileImageArray = when {
            tile.isHidden -> when {
                tile.itemTier != null -> {
                    BingoTileImage.HIDDEN_TIERS[tile.itemTier]?.regular ?: BingoTileImage.FALLBACK.regular
                }
                tile.isFreeSpace -> BingoTileImage.EMPTY.regular
                else -> BingoTileImage.HIDDEN.regular
            }
            tile.isFlashing -> tileImage.flashing
            tile.isAchieved -> tileImage.achieved
            else -> tileImage.regular
        }

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

        // Draw the advancement frame if the tile is an advancement
        if (tile.isAdvancement && !tile.isHidden) {
            for (innerX in 0..innerMax) for (innerY in 0..innerMax) {
                val frameIndex = innerX + innerY * IMAGE_SIZE
                if (frameIndex !in IMAGE_FRAME_ADVANCEMENT.indices) continue
                val color = IMAGE_FRAME_ADVANCEMENT[frameIndex]
                if (color != 0.toByte()) {
                    mapState.setColor(innerX + tileX, innerY + tileY, color)
                }
            }
        }

        // Draw a "+" if the tile can be captured multiple ways
        if (tile.isMultiItem && !tile.isHidden) {
            val offset = 6
            for (innerT in offset-2..offset+2) {
                mapState.setColor(tileX + offset, tileY + innerT, COLOR_WHITE.asByte)
                mapState.setColor(tileX + innerT, tileY + offset, COLOR_WHITE.asByte)
            }
        }

        if (tile.isLocked) {
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

        val teams = tile.teamKeys.mapNotNull { state.teams[it] }
        if (teams.isNotEmpty()) {
            for (innerT in 0..innerMax*2 + 1) {
                val tileTeamX = innerT * teams.size / (innerMax*2 + 1)
                val itemTeam = teams.getOrNull(tileTeamX)?.mapColor ?: continue
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
        map: BingoMap,
        currentView: CardView,
    ) = measureTime {
        val state = map.mapState ?: kotlin.run {
            val state = MapState.of(1, true, server.overworld.registryKey)
            drawMapBorders(state, team)

            mapService.putMapState(map.mapId, state)
            map.mapState = state
            state
        }

        for (x in 0 until 5) {
            for (y in 0 until 5) {
                val drawnTile = map.view.tile(x, y)
                val currentTile = currentView.tile(x, y) ?: continue

                if (drawnTile != currentTile) {
                    drawMapTile(
                        mapState = state,
                        tile = currentTile,
                        itemX = x,
                        itemY = y,
                    )
                }
            }
        }
    }.also {
        if (it >= 1.milliseconds) {
            log.info("Re-drawing map_${team?.map?.mapId} for team ${team?.id?.removePrefix("bingo_")} ($it)")
        }
    }

}