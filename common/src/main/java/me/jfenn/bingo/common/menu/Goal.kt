package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import org.joml.Vector3d

internal const val MENU_GOAL_WIDTH = 2.0

fun MenuComponent.registerGoal(
    position: Vector3d,
    text: TextProvider = koinScope.get(),
) {
    registerTitlePanel(
        position = position + Vector3d(0.0, -0.5, 0.0),
        width = MENU_GOAL_WIDTH,
        title = text.string(StringKey.OptionsGoal),
    )

    val offsetY = 0.6

    registerCardGoal(
        position = position + Vector3d(0.0, -(offsetY + 1*MENU_LINE_HEIGHT), 0.0),
    )

    registerTimer(
        position = position + Vector3d(0.0, -(offsetY + 4*MENU_LINE_HEIGHT + 3*MENU_LINE_PADDING), 0.0),
        width = MENU_GOAL_WIDTH,
    )

    registerTileButton(
        position = position + Vector3d(0.0, -(offsetY + 4*MENU_LINE_HEIGHT + 4*MENU_LINE_PADDING + 0.4), 0.0),
        height = 0.4,
        width = MENU_GOAL_WIDTH,
        text = Text.literal("More  ⋯"),
        blockIdentifier = MENU_BUTTON_ALT_MATERIAL,
    ) {
        // todo
    }
}

fun MenuComponent.registerCardGoal(
    position: Vector3d,
    state: BingoState = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val cardOptions by DelegatedProperty(
        getter = { state.getActiveCard().options },
        setter = {},
    )

    val countProp = DelegatedProperty(
        getter = {
            when (val goal = cardOptions.goal) {
                is BingoGoal.Items -> goal.items
                is BingoGoal.Lines -> goal.lines
            }
        },
        setter = { count ->
            cardOptions.goal = when (cardOptions.goal) {
                is BingoGoal.Items -> BingoGoal.Items(count)
                is BingoGoal.Lines -> BingoGoal.Lines(count)
            }
        }
    )

    registerNumberInput(
        position = position,
        width = MENU_GOAL_WIDTH,
        height = MENU_LINE_HEIGHT,
        valueProp = countProp,
        minValueProp = ConstantProperty(1),
        maxValueProp = computedProperty {
            when (cardOptions.goal) {
                is BingoGoal.Items -> BingoGoal.MAX_ITEMS
                is BingoGoal.Lines -> BingoGoal.MAX_LINES
            }
        },
        format = { cardOptions.goal.format(text) },
    )

    registerTileButton(
        position = position + Vector3d(0.0, -(MENU_LINE_HEIGHT + MENU_LINE_PADDING), 0.0),
        width = MENU_GOAL_WIDTH,
        height = MENU_LINE_HEIGHT,
        text = Text.empty(),
        textProp = computedProperty { Text.literal("⇄ ").append(text.string(cardOptions.goal.string())) },
    ) {
        when (val goal = cardOptions.goal) {
            is BingoGoal.Items -> {
                cardOptions.goal = BingoGoal.Lines(goal.items.coerceAtMost(BingoGoal.MAX_LINES))
            }
            is BingoGoal.Lines -> {
                cardOptions.goal = BingoGoal.Items(goal.lines)
            }
        }
    }
}

fun MenuComponent.registerWinCondition(
    position: Vector3d,
    options: BingoOptions = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
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
        position = position,
        width = MENU_GOAL_WIDTH,
        height = MENU_LINE_HEIGHT,
        valueProp = countProp,
        minValueProp = ConstantProperty(1),
        maxValueProp = computedProperty { 100 },
        format = { text.cardCount(it) },
        isVisible = { options.winCondition is BingoWinCondition.Cards },
    )

    registerTileButton(
        position = position + Vector3d(0.0, -(MENU_LINE_HEIGHT + MENU_LINE_PADDING), 0.0),
        width = MENU_GOAL_WIDTH,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsWinConditionInfinite),
        tooltip = buildTooltip(StringKey.OptionsWinConditionInfinite),
        isActiveProp = computedProperty { options.winCondition is BingoWinCondition.Infinite }
    ) {
        options.winCondition = if (options.winCondition is BingoWinCondition.Infinite) BingoWinCondition.Cards(1) else BingoWinCondition.Infinite
    }
}
