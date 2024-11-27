package me.jfenn.bingo.common.menu

import me.jfenn.bingo.platform.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerGoal(
    position: Vector3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
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
        valueProp = countProp,
        minValueProp = ConstantProperty(1),
        maxValueProp = computedProperty {
            when (cardOptions.goal) {
                is BingoGoal.Items -> BingoGoal.MAX_ITEMS
                is BingoGoal.Lines -> BingoGoal.MAX_LINES
            }
        },
        format = { cardOptions.goal.format(text) },
        permissionGetter = permissionGetter,
    )

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = Text.empty(),
        textProp = computedProperty { Text.literal("⇄ ").append(text.string(cardOptions.goal.string())) },
        permissionGetter = permissionGetter,
    ) {
        when (val goal = cardOptions.goal) {
            is BingoGoal.Items -> {
                cardOptions.goal = BingoGoal.Lines(goal.items.coerceAtMost(BingoGoal.MAX_LINES))
            }
            is BingoGoal.Lines -> {
                cardOptions.goal = BingoGoal.Items(goal.lines)
            }
        }
        markDirty()
    }

    registerEntity(me.jfenn.bingo.platform.EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsGoal)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}