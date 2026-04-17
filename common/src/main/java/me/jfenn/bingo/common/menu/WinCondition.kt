package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.options.BingoWinCondition
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import org.joml.Vector3d

internal fun MenuComponent.registerWinCondition(
    position: Vector3d,
    width: Double = MENU_GOAL_WIDTH,
    optionsService: OptionsService = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = text.string(StringKey.OptionsWinCondition),
    )

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsWinConditionReplaceGoals),
        tooltip = buildTooltip(StringKey.OptionsWinConditionReplaceGoals),
        isActiveProp = computedProperty { optionsService.winCondition is BingoWinCondition.ReplaceGoals }
    ) {
        optionsService.winCondition = BingoWinCondition.ReplaceGoals
    }

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsWinConditionInfinite),
        tooltip = buildTooltip(StringKey.OptionsWinConditionInfinite),
        isActiveProp = computedProperty { optionsService.winCondition is BingoWinCondition.Infinite }
    ) {
        optionsService.winCondition = BingoWinCondition.Infinite
    }

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsWinConditionNumCards),
        tooltip = buildTooltip(StringKey.OptionsWinConditionNumCards),
        isActiveProp = computedProperty { optionsService.winCondition is BingoWinCondition.Cards }
    ) {
        optionsService.winCondition = BingoWinCondition.Cards(1)
    }

    val countProp = DelegatedProperty(
        getter = {
            when (val goal = optionsService.winCondition) {
                is BingoWinCondition.Cards -> goal.cards
                else -> 0
            }
        },
        setter = { count ->
            if (optionsService.winCondition is BingoWinCondition.Cards) {
                optionsService.winCondition = BingoWinCondition.Cards(count)
            }
        }
    )

    registerNumberInput(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        height = MENU_LINE_HEIGHT,
        valueProp = countProp,
        minValueProp = ConstantProperty(1),
        maxValueProp = computedProperty { 100 },
        format = { text.cardCount(it) },
        isVisible = { optionsService.winCondition is BingoWinCondition.Cards },
    )
}
