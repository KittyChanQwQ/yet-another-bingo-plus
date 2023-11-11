package me.jfenn.bingo.map

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.Config
import me.jfenn.bingo.minecraftServer
import net.minecraft.item.map.MapState
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

object MapRenderService {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val COLOR_BACKGROUND = Color(130, 130, 130)
    private val COLOR_ITEM_SHADOW = Color(100, 100, 100)
    private val COLOR_MARGIN = Color(199, 199, 199)
    private val COLOR_SHADOW = Color(20, 20, 20)
    private val COLOR_ACHIEVED = Color(100, 255, 100)
    private val COLOR_ACHIEVED_SHADOW = Color(80, 120, 80)
    private val COLOR_LOCKED = Color(150, 0, 0)

    private const val MAP_SIZE = 128
    private const val MAP_BORDER = 5

    private const val TILE_IMAGE = 16
    private const val TILE_PADDING = 3
    private const val TILE_MARGIN = 2
    private const val TILE_SIZE = TILE_IMAGE + TILE_PADDING*2 + TILE_MARGIN

    class BingoTileImage(
        val regular: Array<Byte> = Array(16*16) { 0 },
        val achieved: Array<Byte> = Array(16*16) { 0 },
    )

    private val imageMap = mutableMapOf<Identifier, BingoTileImage?>()

    fun validateItems() {
        for (item in Registries.ITEM.keys) {
            try {
                readTileImage(item.value).close()
            } catch (e: Throwable) {
                log.warn("Missing item image: ${item.value.path}.png")
            }
        }
    }

    private fun readTileImage(item: Identifier): InputStream {
        return Config.readStream("images/${item.path}.png", false) {
            javaClass.getResourceAsStream("/bingo/images/${item.path}.png")
                ?: throw IllegalArgumentException("Cannot find default texture for ${item.path}.png")
        }
    }

    private fun getTileImage(item: BingoCardEntry): BingoTileImage? {
        imageMap[item.identifier]?.let { return it }

        try {
            val image = readTileImage(item.identifier).use { ImageIO.read(it) }
            val tileImage = BingoTileImage()

            for (x in 0 until TILE_IMAGE) {
                for (y in 0 until TILE_IMAGE) {
                    val i = x + y * TILE_IMAGE
                    var color = Color.fromInt(image.getRGB(x, y))
                    if (color.a == 0) continue

                    // if the image is transparent (glass), blend with the card background
                    if (color.a < 255) color = COLOR_BACKGROUND.mix(color.copy(a = (color.a * 2).coerceAtMost(255)))

                    tileImage.regular[i] = color.asByte
                    // make the item image slightly transparent when achieved
                    tileImage.achieved[i] = COLOR_ACHIEVED.mix(color.copy(a = 200)).asByte
                }
            }

            imageMap[item.identifier] = tileImage
            return tileImage
        } catch (e: Throwable) {
            log.error("Error reading item image: ${item.identifier}", e)
            imageMap[item.identifier] = null
            return null
        }
    }

    fun update(team: BingoTeam, card: BingoCard, config: BingoConfig) = measureTimeMillis {
        val state = MapState.of(1, true, minecraftServer.overworld.registryKey)

        val borderLight = (team.color*4) + 2
        val borderMid = (team.color*4) + 1
        val borderDark = (team.color*4) + 3

        for (x in 0 until MAP_SIZE) {
            for (y in 0 until MAP_SIZE) {
                val i = x + y * MAP_SIZE
                // default to background color
                state.colors[i] = COLOR_BACKGROUND.asByte

                // if x is within a map border
                if (x < MAP_BORDER || y < MAP_BORDER || x >= MAP_SIZE - MAP_BORDER || y >= MAP_SIZE - MAP_BORDER) {
                    state.colors[i] = when {
                        x == 0 || y == 0 -> borderLight
                        x == MAP_SIZE-1 || y == MAP_SIZE-1 -> borderDark
                        x in 1 until MAP_BORDER-1 || y in 1 until MAP_BORDER-1 -> borderMid
                        x in MAP_SIZE - MAP_BORDER + 1 until MAP_SIZE-1 || y in MAP_SIZE - MAP_BORDER + 1 until MAP_SIZE-1 -> borderMid
                        x == MAP_SIZE - MAP_BORDER || y == MAP_SIZE - MAP_BORDER -> borderLight
                        else -> borderDark
                    }.toUByte().toByte()
                    continue
                }

                val itemX = (x - MAP_BORDER) / TILE_SIZE
                val itemY = (y - MAP_BORDER) / TILE_SIZE
                val item = card.entry(itemX, itemY)

                // if the item has been captured, give it a different color
                if (item.hasAchieved(team)) {
                    state.colors[i] = COLOR_ACHIEVED.asByte
                }

                val tileX = (x - MAP_BORDER) % TILE_SIZE
                val tileY = (y - MAP_BORDER) % TILE_SIZE

                if (tileX >= TILE_SIZE - TILE_MARGIN || tileY >= TILE_SIZE - TILE_MARGIN) {
                    if (tileX == TILE_SIZE - TILE_MARGIN || tileY == TILE_SIZE - TILE_MARGIN) {
                        state.colors[i] = COLOR_MARGIN.asByte
                    } else {
                        state.colors[i] = COLOR_SHADOW.asByte
                    }
                    continue
                }

                if (
                    config.drawOtherTeamCompletions
                    && item.teams.isNotEmpty()
                    && (
                        tileX == 0
                        || tileX == TILE_SIZE - TILE_MARGIN - 1
                        || tileY == 0
                        || tileY == TILE_SIZE - TILE_MARGIN - 1
                    )
                ) {
                    val itemTeams = item.teams.filter { it != team }
                    val tileTeamX = (tileX + tileY) * itemTeams.size / ((TILE_SIZE - TILE_MARGIN) * 2)
                    val itemTeam = itemTeams.getOrNull(tileTeamX)
                    if (itemTeam != null) {
                        val variant = if ((tileX + tileY) % 2 == 0) 2 else 3
                        state.colors[i] = ((itemTeam.color*4) + variant).toUByte().toByte()
                        continue
                    }
                }

                // draw the item image in the tile (offset by tile padding)
                val imgX = tileX - TILE_PADDING
                val imgY = tileY - TILE_PADDING

                // draw an X if the item is locked (if BingoMode.LOCKOUT)
                if (imgX in imgY..imgY+1 || (TILE_IMAGE - imgY) in imgX..imgX+1) {
                    if (config.gameMode == BingoMode.LOCKOUT && item.teams.isNotEmpty() && !item.hasAchieved(team)) {
                        state.colors[i] = COLOR_LOCKED.asByte
                        continue
                    }
                }

                if (imgX in 0..TILE_IMAGE && imgY in 0..TILE_IMAGE) {
                    val tileImage = getTileImage(item) ?: continue
                    val tileImageArray = if (item.hasAchieved(team)) tileImage.achieved else tileImage.regular
                    val tileImageIndex = imgX + imgY * TILE_IMAGE

                    if (imgX >= TILE_IMAGE || imgY >= TILE_IMAGE || tileImageArray[tileImageIndex] == 0.toByte()) {
                        if (imgX > 0 && imgY > 0) {
                            // image shadow
                            val prevIndex = (imgX-1) + (imgY-1) * TILE_IMAGE
                            if (prevIndex in tileImageArray.indices && tileImageArray[prevIndex] != 0.toByte())
                                state.colors[i] = if (item.hasAchieved(team)) COLOR_ACHIEVED_SHADOW.asByte else COLOR_ITEM_SHADOW.asByte
                        }

                        continue
                    }

                    state.colors[i] = tileImageArray[tileImageIndex]
                    continue
                }
            }
        }

        state.isDirty = false
        minecraftServer.overworld.putMapState(team.mapName, state)
    }.also {
        log.info("Re-drawing map for team ${team.name} (${it}ms)")
    }

}