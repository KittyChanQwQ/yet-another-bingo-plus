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
    state: BingoState = koinScope.get(),
) {
    val offset = Vector3d()

    registerCardGoal(
        position = position + offset.sub(0.0, MENU_LINE_PADDING, 0.0),
    )

    registerTimer(
        position = position + offset.sub(0.0, 5*MENU_LINE_HEIGHT + 5*MENU_LINE_PADDING, 0.0),
        width = MENU_GOAL_WIDTH,
    )

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + 0.4, 0.0),
        height = 0.4,
        width = MENU_GOAL_WIDTH,
        icon = "⋯",
        text = Text.literal("More"),
        blockIdentifier = MENU_BUTTON_ALT_MATERIAL,
    ) {
        state.menu.page = MenuPage.GOAL
    }
}

fun MenuComponent.registerCardGoal(
    position: Vector3d,
    state: BingoState = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = MENU_GOAL_WIDTH,
        title = text.string(StringKey.OptionsGoal),
    )

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
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
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
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = MENU_GOAL_WIDTH,
        height = MENU_LINE_HEIGHT,
        icon = "⇄",
        text = Text.empty(),
        textProp = computedProperty { text.string(cardOptions.goal.string()) },
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
