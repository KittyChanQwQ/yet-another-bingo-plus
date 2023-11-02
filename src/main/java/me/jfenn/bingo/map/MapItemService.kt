package me.jfenn.bingo.map

import me.jfenn.bingo.NBT_BINGO_IGNORE
import me.jfenn.bingo.NBT_BINGO_VANISH
import me.jfenn.bingo.card.BingoTeam
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object MapItemService {

    fun createMapItem(team: BingoTeam): ItemStack {
        val item = ItemStack(Items.FILLED_MAP, 1)
        item.getOrCreateNbt().apply {
            putInt("map", team.mapId)
            putBoolean(NBT_BINGO_IGNORE, true)
            putBoolean(NBT_BINGO_VANISH, true)
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

}