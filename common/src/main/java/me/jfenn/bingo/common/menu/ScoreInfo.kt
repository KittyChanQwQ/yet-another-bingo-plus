package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import org.joml.Vector3d

internal const val MENU_SCORE_INFO_WIDTH = 2.5
private const val TILE_HEIGHT = 0.425

internal fun MenuComponent.registerScoreInfo(
    position: Vector3d,
    width: Double = MENU_SCORE_INFO_WIDTH,
    options: BingoOptions = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = text.string(StringKey.OptionsScoreInfo),
    )

    registerToggleButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        text = text.string(StringKey.OptionsRemainingTime),
        tooltip = buildTooltip(StringKey.OptionsRemainingTimeTooltip),
        toggleProp = propertyRef(options::showRemainingTime),
    )

    registerToggleButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        text = text.string(StringKey.OptionsCompletedItems),
        tooltip = buildTooltip(StringKey.OptionsCompletedItemsTooltip),
        toggleProp = propertyRef(options::showCompletedItems),
    )

    registerToggleButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        text = text.string(StringKey.OptionsCompletedLines),
        tooltip = buildTooltip(StringKey.OptionsCompletedLinesTooltip),
        toggleProp = propertyRef(options::showCompletedLines),
    )

    registerToggleButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        text = text.string(StringKey.OptionsLeadingTeam),
        tooltip = buildTooltip(StringKey.OptionsLeadingTeamTooltip),
        toggleProp = propertyRef(options::showLeadingTeam),
    )
}