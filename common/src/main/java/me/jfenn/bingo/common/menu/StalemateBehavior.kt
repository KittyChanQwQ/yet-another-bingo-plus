package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.options.StalemateBehavior
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.options.string
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.text.Formatting
import org.joml.Vector3d

internal const val MENU_STALEMATE_WIDTH = 1.5

internal fun MenuComponent.registerStalemateBehavior(
    position: Vector3d,
    width: Double = MENU_STALEMATE_WIDTH,
    optionsService: OptionsService = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = text.string(StringKey.OptionsWinBehaviorWhenStalemate),
    )

    for (behavior in StalemateBehavior.entries) {
        registerTileButton(
            position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
            width = width,
            height = MENU_LINE_HEIGHT,
            text = text.string(behavior.string),
            tooltip = buildList {
                add(text.string(StringKey.OptionsWinBehaviorStalemate, behavior.string).formatted(Formatting.GREEN))
                add(text.string(
                    StringKey.entries
                        .find { it.key == behavior.string.key + ".tooltip" }
                        ?: error("No string found for $behavior tooltip!")
                ))
            },
            isActiveProp = computedProperty { optionsService.stalemateBehavior == behavior }
        ) {
            optionsService.stalemateBehavior = behavior
        }
    }
}
