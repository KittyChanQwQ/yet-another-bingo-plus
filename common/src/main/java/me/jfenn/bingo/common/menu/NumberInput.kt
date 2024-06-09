package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.text.TextProvider
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerNumberInput(
    position: Vec3d,
    valueProp: MutableProperty<Int>,
    minValueProp: Property<Int>,
    maxValueProp: Property<Int>,
    step: Int = 1,
    format: (Int) -> Text = { Text.literal(it.toString()) },
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    var value by valueProp
    val minValue by minValueProp
    val maxValue by maxValueProp

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = Text.literal("+"),
        permissionGetter = permissionGetter,
    ) {
        value = (value + step).coerceIn(minValue, maxValue)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = Text.literal("-"),
        permissionGetter = permissionGetter,
    ) {
        value = (value - step).coerceIn(minValue, maxValue)
        markDirty()
    }

    registerEntity(EntityType.TEXT_DISPLAY, position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
        putString("text", text.serialized(format(value)))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}