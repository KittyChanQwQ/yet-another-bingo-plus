package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.joml.Vector3d

const val TIER_WIDTH = 0.5
const val TIER_OFFSET = -1.0

fun MenuComponent.registerItemDistribution(
    position: Vector3d,
    itemDistributionProp: MutableProperty<List<Int>>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    cardService: CardService = koinScope.get(),
    textSerializer: ITextSerializer = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    var itemDistribution by itemDistributionProp

    fun onUpdate() {
        // when changed, update the bingo card
        cardService.generateCard()
    }

    fun createTier(
        i: Int,
        tier: TierLabel,
    ) {
        registerButton(
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -MENU_ITEM_OFFSET, 0.0, Vector3d()),
            text = Text.literal("+"),
            width = TIER_WIDTH.toFloat(),
            permissionGetter = permissionGetter,
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] + 1).coerceAtMost(25))
            }
            onUpdate()
        }

        registerButton(
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
            text = Text.literal("-"),
            width = TIER_WIDTH.toFloat(),
            permissionGetter = permissionGetter,
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] - 1).coerceAtLeast(0))
            }
            onUpdate()
        }

        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0, Vector3d())
            this.value = Text.literal(tier.name).formatted(tier.formatting, Formatting.BOLD)
            billboard = ITextDisplayEntity.Billboard.FIXED
            alignment = ITextDisplayEntity.TextAlignment.CENTER
            background = 0
            shadow = true
        }

        registerEntity(EntityType.TEXT_DISPLAY) {
            pos = position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0, Vector3d())

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
    }

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsItemDistribution)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
    }

    TierLabel.entries.forEachIndexed { i, tier -> createTier(i, tier) }
}
