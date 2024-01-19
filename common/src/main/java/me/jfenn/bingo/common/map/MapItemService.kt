package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer

class MapItemService(
    private val state: BingoState,
    private val server: MinecraftServer,
) {

    fun createPreviewMapItem(): ItemStack {
        val previewMap = state.getPreviewMap(server)

        val item = ItemStack(Items.FILLED_MAP, 1)
        item.getOrCreateNbt().apply {
            putInt("map", previewMap.mapId)
            putBoolean(NBT_BINGO_IGNORE, true)
            putBoolean(NBT_BINGO_VANISH, true)
            put("display", NbtCompound().apply {
                putString("Name", TextInfo.toJsonString(
                    TextInfo.literal("Preview Bingo Card")
                        .apply { italic = true }
                ))
            })
        }

        return item
    }

    fun createMapItem(team: BingoTeam): ItemStack {
        val teamMap = team.getMap(server)

        val item = ItemStack(Items.FILLED_MAP, 1)
        item.getOrCreateNbt().apply {
            putInt("map", teamMap.mapId)
            putBoolean(NBT_BINGO_IGNORE, true)
            putBoolean(NBT_BINGO_VANISH, true)
            put("display", NbtCompound().apply {
                putString("Name", TextInfo.toJsonString(
                    TextInfo.literal("Bingo Card (${team.name.lowercase()})")
                        .formatted(team.textColor)
                        .apply { italic = true }
                ))
                putInt("MapColor", team.formatting.colorValue ?: 0)
            })
        }

        return item
    }

    fun isMapTeamItem(stack: ItemStack, team: BingoTeam): Boolean {
        val teamMap = team.getMap(server)
        return isMapItem(stack) && stack.nbt?.getInt("map") == teamMap.mapId
    }

    companion object {
        /**
         * Returns true if the item stack is any team's map card
         */
        fun isMapItem(stack: ItemStack): Boolean {
            return stack.item == Items.FILLED_MAP
                    && stack.nbt?.getBoolean(NBT_BINGO_IGNORE) == true
                    && stack.nbt?.getBoolean(NBT_BINGO_VANISH) == true
        }
    }

}