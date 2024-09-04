package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.BingoWinCondition
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerWinCondition(
    position: Vector3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
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
            markDirty()
        }
    )

    registerNumberInput(
        position = position,
        valueProp = countProp,
        minValueProp = ConstantProperty(1),
        maxValueProp = computedProperty { 100 },
        format = { text.cardCount(it) },
        isVisible = { options.winCondition is BingoWinCondition.Cards },
        permissionGetter = permissionGetter,
    )

    val infiniteModeProp = DelegatedProperty(
        getter = {
            options.winCondition is BingoWinCondition.Infinite
        },
        setter = {
            options.winCondition = if (it) BingoWinCondition.Infinite else BingoWinCondition.Cards(1)
            markDirty()
        }
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsWinConditionInfinite),
        tooltip = buildTooltip(StringKey.OptionsWinConditionInfinite),
        toggleProp = infiniteModeProp,
        permissionGetter = permissionGetter,
    )

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsWinCondition)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}