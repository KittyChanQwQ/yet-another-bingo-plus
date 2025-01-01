package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.ITextDisplayEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d

private const val TIER_PADDING = 0.1
private const val TIER_WIDTH = 0.5
private const val TIER_HEIGHT = 0.425
internal const val MENU_ITEM_DISTRIBUTION_WIDTH = TIER_WIDTH*5 + TIER_PADDING*4

internal fun MenuComponent.registerItemDistribution(
    position: Vector3d,
    state: BingoState = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    var itemDistribution by DelegatedProperty(
        getter = { state.getActiveCard().options.itemDistribution },
        setter = {
            val card = state.getActiveCard()
            card.options.itemDistribution = it
            if (card.options.isValid())
                cardService.generateCard(card)
        },
    )

    registerTitlePanel(
        position = position + Vector3d(MENU_ITEM_DISTRIBUTION_WIDTH/2, -MENU_LINE_PADDING - MENU_LINE_HEIGHT, 0.0),
        width = MENU_ITEM_DISTRIBUTION_WIDTH,
        title = text.string(StringKey.OptionsCardDifficulty)
    )

    fun createTier(
        i: Int,
        tier: TierLabel,
    ) {
        val tooltip = buildTooltip(tier.string, tier.formatting)
        val offset = Vector3d(i*(TIER_WIDTH + TIER_PADDING) + TIER_WIDTH/2, -MENU_LINE_HEIGHT - MENU_LINE_PADDING, 0.0)

        registerTileButton(
            position = position + offset.sub(0.0, MENU_LINE_PADDING + TIER_HEIGHT, 0.0),
            width = TIER_WIDTH,
            height = TIER_HEIGHT,
            text = Text.literal("+"),
            tooltip = tooltip,
            isActiveProp = ConstantProperty(true),
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] + 1).coerceAtMost(25))
            }
        }

        val labelPosition = position + offset.sub(0.0, MENU_LINE_PADDING + TIER_HEIGHT, 0.0) +
                Vector3d(0.0, TIER_HEIGHT/2.0 - 1.0/7.0, 0.0)
        val labelText = Text.literal(tier.name).formatted(tier.formatting, Formatting.BOLD)
        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = labelPosition
            this.value = labelText
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            shadow = true
        }

        val valuePosition = position + offset.sub(0.0, MENU_LINE_PADDING + TIER_HEIGHT, 0.0) +
                Vector3d(0.0, TIER_HEIGHT/2.0 - 1.0/7.0, 0.0)
        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = valuePosition

            val color = when {
                itemDistribution.sum() == 25 -> Formatting.WHITE
                itemDistribution.sum() < 25 -> Formatting.YELLOW
                else -> Formatting.RED
            }

            this.value = Text.literal(itemDistribution[i].toString()).formatted(color)

            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            shadow = true
        }

        registerTileButton(
            position = position + offset.sub(0.0, MENU_LINE_PADDING + TIER_HEIGHT, 0.0),
            width = TIER_WIDTH,
            height = TIER_HEIGHT,
            text = Text.literal("-"),
            tooltip = tooltip,
            isActiveProp = ConstantProperty(true),
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] - 1).coerceAtLeast(0))
            }
        }
    }

    registerEntity(EntityType.BLOCK_DISPLAY) {
        pos = position + Vector3d(0.0, -4*MENU_LINE_PADDING - MENU_LINE_HEIGHT - 3*TIER_HEIGHT, -0.051)
        this.blockIdentifier = MENU_BUTTON_ALT_MATERIAL
        brightness = MENU_BRIGHTNESS_OFF
        transformation = Matrix4f().scale(MENU_ITEM_DISTRIBUTION_WIDTH.toFloat(), (2*TIER_HEIGHT + MENU_LINE_PADDING).toFloat(), 0.05f)
    }

    TierLabel.entries.forEachIndexed { i, tier -> createTier(i, tier) }
}
