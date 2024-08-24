package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoGoal
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerGoal(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    val countProp = DelegatedProperty(
        getter = {
            when (val goal = options.cardGoal) {
                is BingoGoal.Items -> goal.items
                is BingoGoal.Lines -> goal.lines
            }
        },
        setter = { count ->
            options.cardGoal = when (options.cardGoal) {
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
            when (options.cardGoal) {
                is BingoGoal.Items -> BingoGoal.MAX_ITEMS
                is BingoGoal.Lines -> BingoGoal.MAX_LINES
            }
        },
        format = { options.cardGoal.format(text) },
        permissionGetter = permissionGetter,
    )

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = Text.empty(),
        textProp = computedProperty { Text.literal("⇄ ").append(text.string(options.cardGoal.string())) },
        permissionGetter = permissionGetter,
    ) {
        when (val goal = options.cardGoal) {
            is BingoGoal.Items -> {
                options.cardGoal = BingoGoal.Lines(goal.items.coerceAtMost(BingoGoal.MAX_LINES))
            }
            is BingoGoal.Lines -> {
                options.cardGoal = BingoGoal.Items(goal.lines)
            }
        }
        markDirty()
    }

    registerEntity(me.jfenn.bingo.api.EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsGoal)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}