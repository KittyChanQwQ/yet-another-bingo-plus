package me.jfenn.bingo.map

import me.jfenn.bingo.card.*
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.map.MapState
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object BingoMapService {

    fun createMapItem(team: BingoTeam): ItemStack {
        val item = ItemStack(Items.FILLED_MAP, 1)
        item.getOrCreateNbt().apply {
            putInt("map", team.mapId)
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

    val COLOR_BACKGROUND = rgb(150, 150, 150).closestColorByte()
    val COLOR_MARGIN = rgb(199, 199, 199).closestColorByte()
    val COLOR_SHADOW = rgb(20, 20, 20).closestColorByte()
    val COLOR_ACHIEVED = rgb(100, 255, 100).closestColorByte()

    val MAP_SIZE = 128
    val MAP_BORDER = 5

    val TILE_IMAGE = 16
    val TILE_PADDING = 3
    val TILE_MARGIN = 2
    val TILE_SIZE = TILE_IMAGE + TILE_PADDING*2 + TILE_MARGIN

    fun update(team: BingoTeam, card: BingoCard) {
        val state = MapState.of(1, true, minecraftServer.overworld.registryKey)

        for (x in 0 until MAP_SIZE) {
            for (y in 0 until MAP_SIZE) {
                val i = x + y * MAP_SIZE
                // default to background color
                state.colors[i] = COLOR_BACKGROUND

                // if x is within a map border
                if (x < MAP_BORDER || y < MAP_BORDER || x >= MAP_SIZE - MAP_BORDER || y >= MAP_SIZE - MAP_BORDER) {
                    val borderLight = (team.color*4) + 2
                    val borderMid = (team.color*4) + 1
                    val borderDark = (team.color*4) + 3
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
                    state.colors[i] = COLOR_ACHIEVED
                }

                val tileX = (x - MAP_BORDER) % TILE_SIZE
                val tileY = (y - MAP_BORDER) % TILE_SIZE

                if (tileX >= TILE_SIZE - TILE_MARGIN || tileY >= TILE_SIZE - TILE_MARGIN) {
                    if (tileX == TILE_SIZE - TILE_MARGIN || tileY == TILE_SIZE - TILE_MARGIN) {
                        state.colors[i] = COLOR_MARGIN
                    } else {
                        state.colors[i] = COLOR_SHADOW
                    }
                    continue
                }

                // draw the item image in the tile (offset by tile padding)
                val imgX = tileX - TILE_PADDING
                val imgY = tileY - TILE_PADDING
                if (imgX in 0 until TILE_IMAGE && imgY in 0 until TILE_IMAGE) {
                    var rgba: Int = item.image?.getRGB(imgX, imgY) ?: continue
                    val color = Color.fromInt(rgba)
                    if (color.a == 0) continue
                    if (color.a < 255) {
                        rgba = Color(150, 150, 150, 255).mix(color).toInt()
                    }

                    state.colors[i] = rgba.closestColorByte()
                    continue
                }

                // TODO: Draw an X if the item is locked (if GameMode.LOCKOUT)
            }
        }

        state.isDirty = false
        minecraftServer.overworld.putMapState(team.mapName, state)
    }

}