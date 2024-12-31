package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import org.joml.Vector3d

internal val CARD_DIFFICULTY_WIDTH = 2.0

fun MenuComponent.registerCardDifficulty(
    position: Vector3d,
    config: BingoConfig = koinScope.get(),
    state: BingoState = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerTitlePanel(
        position = position + Vector3d(0.0, -0.5, 0.0),
        width = CARD_DIFFICULTY_WIDTH,
        title = text.string(StringKey.OptionsItemDistribution),
    )

    val difficultyPresets = config.difficultyPresets.entries.take(4)

    for ((i, entry) in difficultyPresets.withIndex()) {
        val (name, preset) = entry
        registerTileButton(
            position = position + Vector3d(0.0, -(0.5 + (i+1)*(MENU_LINE_PADDING + MENU_LINE_HEIGHT)), 0.0),
            width = CARD_DIFFICULTY_WIDTH,
            height = MENU_LINE_HEIGHT,
            text = TierLabel.presetText(name),
            isActiveProp = computedProperty {
                state.getActiveCard().options.itemDistribution == preset
            },
        ) {
            val cardOptions = state.getActiveCard().options
            cardOptions.itemDistribution = preset

            // when changed, update the bingo card
            cardService.generateCard()
        }
    }

    registerTileButton(
        position = position + Vector3d(0.0, -(0.6 + 4*MENU_LINE_HEIGHT + 4*MENU_LINE_PADDING + 0.4), 0.0),
        height = 0.4,
        width = CARD_DIFFICULTY_WIDTH,
        text = Text.literal("Customize  ⋯"),
        blockIdentifier = MENU_BUTTON_ALT_MATERIAL,
    ) {
        // todo
    }
}