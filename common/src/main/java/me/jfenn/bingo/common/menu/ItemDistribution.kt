package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.TierList
import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

const val TIER_WIDTH = 0.5
const val TIER_OFFSET = -1.0

fun MenuComponent.registerItemDistribution(
    position: Vec3d,
    itemDistributionProp: MutableProperty<List<Int>>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    cardService: CardService = koinScope.get(),
) {
    var itemDistribution by itemDistributionProp

    fun onUpdate() {
        // when changed, update the bingo card
        cardService.generateCard()
    }

    fun createTier(
        i: Int,
        tier: TierList,
    ) {
        registerButton(
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -MENU_ITEM_OFFSET, 0.0),
            text = "+",
            width = TIER_WIDTH.toFloat(),
            permissionGetter = permissionGetter,
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] + 1).coerceAtMost(25))
            }
            onUpdate()
        }

        registerButton(
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
            text = "-",
            width = TIER_WIDTH.toFloat(),
            permissionGetter = permissionGetter,
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] - 1).coerceAtLeast(0))
            }
            onUpdate()
        }

        registerEntity(EntityType.TEXT_DISPLAY, position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
            putString("text", TextInfo.toJsonString(
                TextInfo.literal(tier.name)
                    .formatted(tier.formatting)
                    .apply { bold = true }
            ))
            putString("billboard", "fixed")
            putString("alignment", "center")
            putInt("background", 0)
            putBoolean("shadow", true)
        }

        registerEntity(EntityType.TEXT_DISPLAY, position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0)) {
            val color = when {
                itemDistribution.sum() == 25 -> TextColor.WHITE
                itemDistribution.sum() < 25 -> TextColor.YELLOW
                else -> TextColor.RED
            }

            putString("text", TextInfo.toJsonString(
                TextInfo.literal(itemDistribution[i].toString())
                    .formatted(color)
            ))

            putString("billboard", "fixed")
            putString("alignment", "center")
            putInt("background", 0)
            putBoolean("shadow", true)
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Item Distribution\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    TierList.entries.forEachIndexed { i, tier -> createTier(i, tier) }
}
