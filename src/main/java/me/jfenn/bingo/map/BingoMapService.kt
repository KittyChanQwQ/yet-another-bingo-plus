package me.jfenn.bingo.map

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.minecraftServer
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.map.MapState
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

object BingoMapService {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun createMapItem(team: BingoTeam): ItemStack {
        val item = ItemStack(Items.FILLED_MAP, 1)
        item.getOrCreateNbt().apply {
            putInt("map", team.mapId)
            putBoolean("bingo_ignore", true)
            put("display", NbtCompound().apply {
                putString("Name", Text.Serializer.toJson(
                    Text.literal("Bingo Card (${team.name.lowercase()})")
                        .formatted(team.formatting, Formatting.ITALIC)
                ))
            })
        }

        return item
    }

    /**
     * Returns true if the item stack is a team's map card
     */
    fun isMapItem(stack: ItemStack): Boolean {
        return stack.item == Items.FILLED_MAP && BingoTeam.values().any { FilledMapItem.getMapId(stack) == it.mapId }
    }

    private val COLOR_BACKGROUND = Color(130, 130, 130)
    private val COLOR_ITEM_SHADOW = Color(100, 100, 100)
    private val COLOR_MARGIN = Color(199, 199, 199)
    private val COLOR_SHADOW = Color(20, 20, 20)
    private val COLOR_ACHIEVED = Color(100, 255, 100)
    private val COLOR_LOCKED = Color(150, 0, 0)

    private const val MAP_SIZE = 128
    private const val MAP_BORDER = 5

    private const val TILE_IMAGE = 16
    private const val TILE_PADDING = 3
    private const val TILE_MARGIN = 2
    private const val TILE_SIZE = TILE_IMAGE + TILE_PADDING*2 + TILE_MARGIN

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
                        x == MAP_SIZE-1 || y == MAP_SIZE-1 -> (team.color*4) + 3
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

                if (imgX in 0 until TILE_IMAGE+1 && imgY in 0 until TILE_IMAGE+1) {
                    var rgba: Int = if (imgX < TILE_IMAGE && imgY < TILE_IMAGE) {
                        item.image?.getRGB(imgX, imgY) ?: continue
                    } else 0

                    val color = Color.fromInt(rgba)
                    if (color.a == 0) {
                        // image shadow
                        if (imgX > 0 && imgY > 0 && item.image?.getRGB(imgX-1, imgY-1)?.let { Color.fromInt(it) }?.a != 0)
                            state.colors[i] = COLOR_ITEM_SHADOW.asByte
                        continue
                    }

                    if (color.a < 255) {
                        // if the image is transparent (glass), blend with the card background
                        rgba = COLOR_BACKGROUND.mix(color).toInt()
                    }

                    if (item.hasAchieved(team)) {
                        // make the item image slightly transparent when achieved
                        rgba = COLOR_ACHIEVED.mix(color.copy(a = 200)).toInt()
                    }

                    state.colors[i] = rgba.closestColorByte()
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