package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.platform.IPlayerHandle
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Vector3d
import kotlin.math.ceil

private class RadioMenuState(
    var selectedPage: Int
)

internal fun MenuComponent.registerRadioMenu(
    position: Vector3d,
    width: Double,
    height: Double,
    title: MutableText,
    options: List<Text>,
    optionsPerPage: Int = (height / (MENU_LINE_HEIGHT + MENU_LINE_PADDING)).toInt() - 2,
    selectedIndexProp: MutableProperty<Int>,
    onChange: (IPlayerHandle, Int) -> Unit = { _, _ -> },
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, 2*MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = title,
    )

    var selectedIndex by selectedIndexProp
    val pageState = RadioMenuState(
        selectedPage = selectedIndex / optionsPerPage
    )
    val pagesLength = ceil(options.size / optionsPerPage.toFloat()).toInt()

    val optionHeight = ((height - 3*MENU_LINE_PADDING - 2*MENU_LINE_HEIGHT) / optionsPerPage) - MENU_LINE_PADDING
    for (i in 0 until optionsPerPage) {
        val itemIndex by computedProperty {
            (pageState.selectedPage*optionsPerPage + i).takeIf { it in options.indices }
        }

        registerTileButton(
            position = position + offset.sub(0.0, MENU_LINE_PADDING + optionHeight, 0.0),
            width = width,
            height = optionHeight,
            text = Text.empty(),
            textProp = computedProperty {
                itemIndex?.let { options[it] } ?: Text.empty()
            },
            isActiveProp = computedProperty { selectedIndex == itemIndex },
        ) { player ->
            itemIndex?.also { selectedIndex = it }?.also { onChange(player, it) }
        }
    }

    registerNumberInput(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        height = MENU_LINE_HEIGHT,
        valueProp = propertyRef(pageState::selectedPage),
        minValueProp = ConstantProperty(0),
        maxValueProp = ConstantProperty(pagesLength-1),
        isVisible = { options.size > optionsPerPage },
        increaseLabel = "»",
        decreaseLabel = "«",
        format = { page ->
            (0 until pagesLength)
                .map { i ->
                    val hasSelected = selectedIndex in i*optionsPerPage until (i+1)*optionsPerPage
                    when {
                        page == i -> Text.literal(if (hasSelected) "■" else "□").formatted(Formatting.WHITE)
                        else -> Text.literal(if (hasSelected) "◆" else "◇").formatted(Formatting.GRAY)
                    }
                }
                .joinText(Text.literal(" "))
        }
    )
}
