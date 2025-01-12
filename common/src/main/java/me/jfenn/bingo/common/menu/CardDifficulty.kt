package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Vector3d

internal const val CARD_DIFFICULTY_WIDTH = 1.6

internal fun MenuComponent.registerCardDifficulty(
    position: Vector3d,
    config: BingoConfig = koinScope.get(),
    state: BingoState = koinScope.get(),
    optionsService: OptionsService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerTitlePanel(
        position = position + Vector3d(0.0, -0.5, 0.0),
        width = CARD_DIFFICULTY_WIDTH,
        title = text.string(StringKey.OptionsCardDifficulty),
    )

    val difficultyPresets = config.difficultyPresets.entries.take(4)

    for ((i, entry) in difficultyPresets.withIndex()) {
        val (name, preset) = entry
        registerTileButton(
            position = position + Vector3d(0.0, -(0.5 + (i+1)*(MENU_LINE_PADDING + MENU_LINE_HEIGHT)), 0.0),
            width = CARD_DIFFICULTY_WIDTH,
            height = MENU_LINE_HEIGHT,
            text = TierLabel.presetText(name),
            tooltip = buildList {
                add(
                    text.string(StringKey.OptionsCardDifficulty)
                        .append(": ")
                        .append(TierLabel.presetText(name))
                        .formatted(Formatting.GREEN)
                )
                add(text.string(StringKey.OptionsCardDifficultyTooltip))
                add(
                    TierLabel.entries
                        .mapIndexed { index, label ->
                            Text.literal(preset.getOrNull(index).toString())
                                .formatted(label.formatting)
                        }
                        .joinText(Text.literal(" "))
                )
            },
            isActiveProp = computedProperty {
                state.getActiveCard().options.itemDistribution == preset
            },
        ) {
            optionsService.setCardDifficulty(
                ctx = OptionsService.Context(it),
                card = state.getActiveCard(),
                itemDistInput = preset,
            )
        }
    }

    registerTileButton(
        position = position + Vector3d(0.0, -(0.6 + 4*MENU_LINE_HEIGHT + 4*MENU_LINE_PADDING + 0.4), 0.0),
        height = 0.4,
        width = CARD_DIFFICULTY_WIDTH,
        icon = "⋯",
        text = text.string(StringKey.OptionsCustomize),
        blockIdentifier = MENU_BUTTON_ALT_MATERIAL,
    ) {
        state.menu.page = MenuPage.ITEMS
    }
}