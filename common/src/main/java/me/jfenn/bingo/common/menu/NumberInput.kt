package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.IDisplayEntity
import me.jfenn.bingo.platform.ITextDisplayEntity
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Vector3d

internal fun MenuComponent.registerNumberInput(
    position: Vector3d,
    width: Double = 2.0,
    height: Double = MENU_LINE_HEIGHT,
    valueProp: MutableProperty<Int>,
    minValueProp: Property<Int>,
    maxValueProp: Property<Int>,
    step: Int = 1,
    format: (Int) -> Text = { Text.literal(it.toString()) },
    tooltip: List<Text>? = null,
    isVisible: () -> Boolean = { true },
    decreaseLabel: String = "-",
    increaseLabel: String = "+",
) {
    var value by valueProp
    val minValue by minValueProp
    val maxValue by maxValueProp

    val increaseText = Text.literal(increaseLabel)
    registerTileButton(
        position = position + Vector3d(width/2 - height/2, 0.0, 0.0),
        width = height,
        height = height,
        text = Text.empty(),
        textProp = computedProperty {
            increaseText.takeIf { isVisible() } ?: Text.empty()
        },
        isActiveProp = ConstantProperty(true),
        tooltip = tooltip,
    ) {
        value = (((value / step) + 1) * step).coerceIn(minValue, maxValue)
    }

    val decreaseText = Text.literal(decreaseLabel)
    registerTileButton(
        position = position + Vector3d(-width/2 + height/2, 0.0, 0.0),
        width = height,
        height = height,
        text = Text.empty(),
        textProp = computedProperty {
            decreaseText.takeIf { isVisible() } ?: Text.empty()
        },
        isActiveProp = ConstantProperty(true),
        tooltip = tooltip,
    ) {
        value = (((value / step) - 1) * step).coerceIn(minValue, maxValue)
    }

    val backgroundWidth = width - 2*height - 0.2
    registerEntity(EntityType.BLOCK_DISPLAY) {
        pos = position + Vector3d(-backgroundWidth/2, 0.0, -0.051)
        blockIdentifier = if (isVisible()) MENU_BUTTON_MATERIAL else "minecraft:air"
        brightness = MENU_BRIGHTNESS_OFF
        transformation = Matrix4f().scale(backgroundWidth.toFloat(), height.toFloat(), 0.05f)
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position + Vector3d(0.0, height/2 - 0.1, 0.0)
        this.value = format(value).takeIf { isVisible() } ?: Text.empty()
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}