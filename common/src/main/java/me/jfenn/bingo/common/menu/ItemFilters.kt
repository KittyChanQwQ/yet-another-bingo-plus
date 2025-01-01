package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import org.joml.Vector3d

internal const val MENU_ITEM_FILTERS_WIDTH = 2.0

fun MenuComponent.registerItemFilters(
    position: Vector3d,
    width: Double = MENU_ITEM_FILTERS_WIDTH,
    height: Double,
    state: BingoState = koinScope.get(),
    cardService: CardService = koinScope.get(),
    objectiveFilterService: ObjectiveFilterService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val presets = objectiveFilterService.getPresetFilters()

    val selectedIndexProp = DelegatedProperty(
        getter = {
            presets.values.indexOfFirst { itemFilter ->
                state.getActiveCard().options.itemFilter == itemFilter
            }
        },
        setter = { index ->
            val preset = presets.values.elementAtOrNull(index)
            if (preset != null) {
                state.getActiveCard().options.itemFilter = preset

                // when changed, update the bingo card
                cardService.generateCard()
            }
        }
    )

    registerRadioMenu(
        position = position,
        width = width,
        height = height,
        title = text.string(StringKey.OptionsFilter),
        options = presets.keys.map {
            ObjectiveFilterList.formatName(it)
        },
        selectedIndexProp = selectedIndexProp,
    )
}