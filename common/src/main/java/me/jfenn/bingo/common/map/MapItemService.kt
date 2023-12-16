package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound

object MapItemService {

    fun createMapItem(ctx: BingoContext, team: BingoTeam): ItemStack {
        val item = ItemStack(Items.FILLED_MAP, 1)
        item.getOrCreateNbt().apply {
            putInt("map", team.getMapId(ctx))
            putBoolean(NBT_BINGO_IGNORE, true)
            putBoolean(NBT_BINGO_VANISH, true)
            put("display", NbtCompound().apply {
                putString("Name", TextInfo.toJsonString(
                    TextInfo.literal("Bingo Card (${team.name.lowercase()})")
                        .formatted(team.textColor)
                        .apply { italic = true }
                ))
            })
        }

        return item
    }

    /**
     * Returns true if the item stack is a team's map card
     */
    fun isMapItem(stack: ItemStack): Boolean {
        return stack.item == Items.FILLED_MAP
                && stack.nbt?.getBoolean(NBT_BINGO_IGNORE) == true
                && stack.nbt?.getBoolean(NBT_BINGO_VANISH) == true
    }

    fun isMapTeamItem(ctx: BingoContext, stack: ItemStack, team: BingoTeam): Boolean {
        return isMapItem(stack) && stack.nbt?.getInt("map") == team.getMapId(ctx)
    }

}