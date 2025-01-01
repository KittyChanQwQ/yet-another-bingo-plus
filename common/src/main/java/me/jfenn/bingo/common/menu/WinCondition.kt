package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import org.joml.Vector3d

internal fun MenuComponent.registerWinCondition(
    position: Vector3d,
    width: Double = MENU_GOAL_WIDTH,
    options: BingoOptions = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = text.string(StringKey.OptionsWinCondition),
    )

    val countProp = DelegatedProperty(
        getter = {
            when (val goal = options.winCondition) {
                is BingoWinCondition.Cards -> goal.cards
                is BingoWinCondition.Infinite -> 0
            }
        },
        setter = { count ->
            options.winCondition = when (options.winCondition) {
                is BingoWinCondition.Cards -> BingoWinCondition.Cards(count)
                is BingoWinCondition.Infinite -> BingoWinCondition.Infinite
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
        isVisible = { options.winCondition is BingoWinCondition.Cards },
    )

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsWinConditionInfinite),
        tooltip = buildTooltip(StringKey.OptionsWinConditionInfinite),
        isActiveProp = computedProperty { options.winCondition is BingoWinCondition.Infinite }
    ) {
        options.winCondition = if (options.winCondition is BingoWinCondition.Infinite) BingoWinCondition.Cards(1) else BingoWinCondition.Infinite
    }
}
