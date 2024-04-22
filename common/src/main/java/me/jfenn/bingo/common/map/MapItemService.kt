package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class MapItemService(
    private val state: BingoState,
    private val itemStackFactory: IItemStackFactory,
    private val mapService: IMapService,
) {

    fun createPreviewMapItem(): IItemStack {
        val previewMap = state.getPreviewMap(mapService)

        val item = itemStackFactory.createFilledMap().apply {
            mapId = previewMap.mapId
            setDisplay(
                name = Text.literal("Preview Bingo Card").formatted(Formatting.ITALIC),
                lore = null,
            )
            addCustomTag(NBT_BINGO_IGNORE)
            addCustomTag(NBT_BINGO_VANISH)
        }

        return item
    }

    fun isPreviewMapItem(stack: IItemStack): Boolean {
        val previewMap = state.getPreviewMap(mapService)

        return stack.asFilledMap()?.mapId == previewMap.mapId
    }

    fun createMapItem(team: BingoTeam): IItemStack {
        val teamMap = team.getMap(mapService)

        val item = itemStackFactory.createFilledMap().apply {
            mapId = teamMap.mapId
            mapColor = team.formatting.colorValue
            setDisplay(
                name = Text.literal("Bingo Card (${team.name.lowercase()})")
                    .formatted(team.formatting, Formatting.ITALIC),
                lore = null,
            )
            addCustomTag(NBT_BINGO_IGNORE)
            addCustomTag(NBT_BINGO_VANISH)
        }

        return item
    }

    fun isMapTeamItem(stack: IItemStack, team: BingoTeam): Boolean {
        val teamMap = team.getMap(mapService)

        return isMapItem(stack) && stack.asFilledMap()?.mapId == teamMap.mapId
    }

    /**
     * Returns true if the item stack is any team's map card
     */
    fun isMapItem(stack: IItemStack): Boolean {
        return stack.asFilledMap() != null
                && stack.hasCustomTag(NBT_BINGO_IGNORE)
                && stack.hasCustomTag(NBT_BINGO_VANISH)
    }

}