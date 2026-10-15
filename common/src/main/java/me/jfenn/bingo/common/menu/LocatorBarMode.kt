package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.options.LocatorBarMode
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.options.string
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.text.Formatting
import org.joml.Vector3d

internal const val MENU_LOCATOR_BAR_WIDTH = 1.6

internal fun MenuComponent.registerLocatorBarMode(
    position: Vector3d,
    width: Double = MENU_LOCATOR_BAR_WIDTH,
    optionsService: OptionsService = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = text.string(StringKey.OptionsLocatorBar),
    )

    for (mode in LocatorBarMode.entries) {
        registerTileButton(
            position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
            width = width,
            height = MENU_LINE_HEIGHT,
            text = text.string(mode.string),
            tooltip = buildList {
                add(text.string(StringKey.OptionsLocatorBarMode, mode.string).formatted(Formatting.GREEN))
                add(text.string(
                    StringKey.entries
                        .find { it.key == mode.string.key + ".tooltip" }
                        ?: error("No string found for $mode tooltip!")
                ))
            },
            isActiveProp = computedProperty { optionsService.locatorBarMode == mode }
        ) {
            optionsService.locatorBarMode = mode
        }
    }
}
