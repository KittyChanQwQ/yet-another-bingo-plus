package me.jfenn.bingo.common.menu

import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.ITextDisplayEntity
import me.jfenn.bingo.common.utils.plus
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerNumberInput(
    position: Vector3d,
    valueProp: MutableProperty<Int>,
    minValueProp: Property<Int>,
    maxValueProp: Property<Int>,
    step: Int = 1,
    format: (Int) -> Text = { Text.literal(it.toString()) },
    tooltip: List<Text>? = null,
    isVisible: () -> Boolean = { true },
    permissionGetter: (ServerPlayerEntity) -> Boolean,
) {
    var value by valueProp
    val minValue by minValueProp
    val maxValue by maxValueProp

    registerButton(
        position = position + Vector3d(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = Text.empty(),
        textProp = computedProperty {
            Text.literal("+").takeIf { isVisible() } ?: Text.empty()
        },
        tooltip = tooltip,
        permissionGetter = permissionGetter,
    ) {
        value = (value + step).coerceIn(minValue, maxValue)
        markDirty()
    }

    registerButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = Text.empty(),
        textProp = computedProperty {
            Text.literal("-").takeIf { isVisible() } ?: Text.empty()
        },
        tooltip = tooltip,
        permissionGetter = permissionGetter,
    ) {
        value = (value - step).coerceIn(minValue, maxValue)
        markDirty()
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)
        this.value = format(value).takeIf { isVisible() } ?: Text.empty()
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}