package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IItemStack
import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class MapItemService(
    private val state: BingoState,
    private val server: MinecraftServer,
    private val mapService: IMapService,
) {

    fun createPreviewMapItem(): IItemStack {
        val previewMap = state.getPreviewMap(server)

        val item = mapService.createFilledMap(previewMap.mapId, null).apply {
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
        val previewMap = state.getPreviewMap(server)

        return isMapItem(stack) && mapService.getMapId(stack) == previewMap.mapId
    }

    fun createMapItem(team: BingoTeam): IItemStack {
        val teamMap = team.getMap(server)

        val item = mapService.createFilledMap(
            mapId = teamMap.mapId,
            mapColor = team.formatting.colorValue,
        ).apply {
            setDisplay(
                name = Text.literal("Bingo Card (${team.name.lowercase()})")
                    .formatted(team.formatting, Formatting.ITALIC),
                lore = null,
            )

        }

        return item
    }

    fun isMapTeamItem(stack: IItemStack, team: BingoTeam): Boolean {
        val teamMap = team.getMap(server)

        return isMapItem(stack) && mapService.getMapId(stack) == teamMap.mapId
    }

    /**
     * Returns true if the item stack is any team's map card
     */
    fun isMapItem(stack: IItemStack): Boolean {
        return mapService.isFilledMap(stack)
                && stack.hasCustomTag(NBT_BINGO_IGNORE)
                && stack.hasCustomTag(NBT_BINGO_VANISH)
    }

}