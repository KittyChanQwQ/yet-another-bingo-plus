package me.jfenn.bingo.common.map

import kotlinx.datetime.Clock
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.state.BingoContext
import net.minecraft.item.FilledMapItem
import net.minecraft.item.Item
import net.minecraft.item.map.MapState
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

object MapRenderService {

    private val log = LoggerFactory.getLogger(this::class.java)

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

    private val FLASHING_DURATION = 4.seconds

    class BingoTileImage(
        val regular: Array<Byte> = Array(16*16) { 0 },
        val achieved: Array<Byte> = Array(16*16) { 0 },
    )

    private val imageMap = WeakHashMap<Item, BingoTileImage?>()

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

    private fun getTileImage(entry: BingoCardEntry): BingoTileImage? {
        val item = entry.visualItem ?: return null
        if (imageMap.containsKey(item))
            return imageMap[item]

        try {
            val identifier = Registries.ITEM.getId(item)
            val image = readTileImage(identifier).use { ImageIO.read(it) }
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
        state: MapState,
        team: BingoTeam?,
    ) {
        val borderLight = (getMapColor(team)*4 + 2).toUByte().toByte()
        val borderMid = (getMapColor(team)*4 + 1).toUByte().toByte()
        val borderDark = (getMapColor(team)*4 + 3).toUByte().toByte()

        // Outer edge borders
        for (x in 0 until MAP_SIZE) {
            state.setColor(x, 0, borderLight)
            state.setColor(0, x, borderLight)

            state.setColor(x, MAP_SIZE-1, borderDark)
            state.setColor(MAP_SIZE-1, x, borderDark)
        }

        // Middle borders
        for (x in 1 until MAP_SIZE-1) {
            for (y in 1 until MAP_BORDER-1) {
                val yInv = MAP_SIZE - y - 1
                state.setColor(x, y, borderMid)
                state.setColor(y, x, borderMid)
                state.setColor(x, yInv, borderMid)
                state.setColor(yInv, x, borderMid)
            }
        }

        // Inner borders
        for (x in MAP_BORDER-1..MAP_SIZE-MAP_BORDER) {
            state.setColor(x, MAP_BORDER-1, borderDark)
            state.setColor(MAP_BORDER-1, x, borderDark)
            state.setColor(x, MAP_SIZE-MAP_BORDER, borderLight)
            state.setColor(MAP_SIZE-MAP_BORDER, x, borderLight)
        }

        // Cell borders
        for (x in MAP_BORDER until MAP_SIZE-MAP_BORDER) {
            for (y in 1..4) {
                val yActual = MAP_BORDER + y*TILE_SIZE - TILE_MARGIN + 1
                state.setColor(x, yActual, COLOR_SHADOW.asByte)
                state.setColor(yActual, x, COLOR_SHADOW.asByte)
            }
        }
        for (x in MAP_BORDER until MAP_SIZE-MAP_BORDER) {
            for (y in 1..4) {
                val yActual = MAP_BORDER + y*TILE_SIZE - TILE_MARGIN
                state.setColor(x, yActual, COLOR_MARGIN.asByte)
                state.setColor(yActual, x, COLOR_MARGIN.asByte)
            }
        }
    }

    private fun drawMapTile(
        ctx: BingoContext,
        team: BingoTeam?,
        state: MapState,
        item: BingoCardEntry,
        itemX: Int,
        itemY: Int,
    ) {
        require(itemX in 0 until 5)
        require(itemY in 0 until 5)

        val isAchieved = team != null && item.hasAchieved(team)
        val isFlashing = isFlashing(team, item)
                && team != null
                && Clock.System.now() - item.updatedAt(team) < 4.seconds
                && (ctx.server.ticks / 10) % 2 == 0

        val isLocked = ctx.config.isLockoutMode && item.teams.isNotEmpty() && team != null && !item.hasAchieved(team)
        val tileImage = getTileImage(item)
        val tileImageArray = when {
            ctx.config.hiddenItems && (team == null || !item.hasSeen(team)) -> null
            isAchieved && !isFlashing -> tileImage?.achieved
            else -> tileImage?.regular
        }

        val itemTeams = item.teams.mapNotNull { ctx.state.teams[it] }
            .filter { it.key != team?.key }

        val tileX = MAP_BORDER + TILE_SIZE*itemX
        val tileY = MAP_BORDER + TILE_SIZE*itemY

        val innerMax = TILE_SIZE - TILE_MARGIN - 1
        for (innerX in 0..innerMax) {
            val x = tileX + innerX
            for (innerY in 0..innerMax) {
                val y = tileY + innerY

                // default to background color
                state.setColor(x, y, when {
                    isFlashing -> COLOR_FLASHING.asByte
                    isAchieved -> COLOR_ACHIEVED.asByte
                    else -> COLOR_BACKGROUND.asByte
                })

                if (
                    ctx.config.showOtherTeamCompletions &&
                    item.hasAnyAchieved() &&
                    (innerX == 0 || innerX == innerMax || innerY == 0 || innerY == innerMax)
                ) {
                    val tileTeamX = (innerX + innerY) * itemTeams.size / ((TILE_SIZE - TILE_MARGIN) * 2)
                    val itemTeam = itemTeams.getOrNull(tileTeamX)
                    if (itemTeam != null) {
                        val variant = if ((innerX + innerY) % 2 == 0) 2 else 3
                        state.setColor(x, y, (getMapColor(itemTeam)*4 + variant).toUByte().toByte())
                        continue
                    }
                }

                // draw the item image in the tile (offset by tile padding)
                val imgX = innerX - TILE_PADDING
                val imgY = innerY - TILE_PADDING

                // draw an X if the item is locked (if BingoMode.LOCKOUT)
                if (imgX in imgY..imgY+1 || (TILE_IMAGE - imgY) in imgX..imgX+1) {
                    if (isLocked) {
                        state.setColor(x, y, COLOR_LOCKED.asByte)
                        continue
                    }
                }

                if (imgX in 0..TILE_IMAGE && imgY in 0..TILE_IMAGE && tileImageArray != null) {
                    val tileImageIndex = imgX + imgY * TILE_IMAGE

                    if (imgX >= TILE_IMAGE || imgY >= TILE_IMAGE || tileImageArray[tileImageIndex] == 0.toByte()) {
                        if (imgX > 0 && imgY > 0) {
                            // image shadow
                            val prevIndex = (imgX-1) + (imgY-1) * TILE_IMAGE
                            if (prevIndex in tileImageArray.indices && tileImageArray[prevIndex] != 0.toByte()) {
                                state.setColor(x, y, when {
                                    isAchieved && !isFlashing -> COLOR_ACHIEVED_SHADOW.asByte
                                    else -> COLOR_ITEM_SHADOW.asByte
                                })
                            }
                        }

                        continue
                    }

                    state.setColor(x, y, tileImageArray[tileImageIndex])
                    continue
                }
            }
        }
    }

    fun isFlashing(team: BingoTeam?, entry: BingoCardEntry) : Boolean {
        return team != null
                && entry.hasAchieved(team)
                && Clock.System.now() - entry.updatedAt(team) < (FLASHING_DURATION + 1.seconds)
    }

    fun update(
        ctx: BingoContext,
        team: BingoTeam?,
    ) = measureTime {
        val card = ctx.state.card
        val map = team?.getMap(ctx) ?: ctx.state.getPreviewMap(ctx)

        val state = map.mapState ?: kotlin.run {
            val state = MapState.of(1, true, ctx.server.overworld.registryKey)
            drawMapBorders(state, team)

            ctx.server.overworld.putMapState(FilledMapItem.getMapName(map.mapId), state)
            map.mapState = state
            state
        }

        for (x in 0 until 5) {
            for (y in 0 until 5) {
                val entry = card.entry(x, y)
                if (
                    entry.updatedAt(team) > map.updatedAt ||
                    (ctx.config.showOtherTeamCompletions && entry.updatedAt > map.updatedAt) ||
                    (isFlashing(team, entry) && ctx.server.ticks % 10 == 0)
                ) {
                    drawMapTile(
                        ctx = ctx,
                        state = state,
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