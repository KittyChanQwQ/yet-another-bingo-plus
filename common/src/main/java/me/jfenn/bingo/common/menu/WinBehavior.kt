package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.options.EndWhen
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import org.joml.Vector3d

private const val TILE_HEIGHT = 0.6
internal const val MENU_WIN_BEHAVIOR_WIDTH = 3.0

internal fun MenuComponent.registerWinBehavior(
    position: Vector3d,
    width: Double = MENU_WIN_BEHAVIOR_WIDTH,
    options: BingoOptions = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = width,
        title = text.string(StringKey.OptionsWinBehavior),
    )

    registerToggleButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        text = text.string(StringKey.OptionsWinBehaviorSkipCardWhenLost),
        tooltip = buildTooltip(StringKey.OptionsWinBehaviorSkipCardWhenLost),
        toggleProp = propertyRef(options::skipCardWhenLost),
    )

    registerToggleButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        text = text.string(StringKey.OptionsWinBehaviorEndWhenStalemate),
        tooltip = buildTooltip(StringKey.OptionsWinBehaviorEndWhenStalemate),
        toggleProp = propertyRef(options::endGameWhenStalemate),
    )

    val toggleEndWhen = arrayOf(EndWhen.FirstWin, EndWhen.AllWin, EndWhen.Never)
    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + TILE_HEIGHT, 0.0),
        width = width,
        height = TILE_HEIGHT,
        icon = "⇄",
        text = Text.empty(),
        textProp = computedProperty {
            text.string(StringKey.OptionsEndWhen, options.endGameWhen.string(text))
        },
        tooltip = buildTooltip(StringKey.OptionsWinBehaviorEndWhen),
    ) {
        val next = toggleEndWhen
            .indexOfFirst { it::class == options.endGameWhen::class }
            .plus(1)
            .mod(toggleEndWhen.size)
            .coerceIn(toggleEndWhen.indices)

        options.endGameWhen = toggleEndWhen[next]
    }
}