package me.jfenn.bingo.menu

import me.jfenn.bingo.config.TierList
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d

const val TIER_WIDTH = 0.5
const val TIER_OFFSET = -1.0

fun MenuComponent.registerItemDistribution(
    position: Vec3d,
    itemDistributionProp: MutableProperty<List<Int>>,
    requiredPermissionLevel: Int = 2,
) {
    var itemDistribution by itemDistributionProp

    fun createTier(
        i: Int,
        tier: TierList,
    ) {
        registerButton(
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -MENU_ITEM_OFFSET, 0.0),
            text = "+",
            requiredPermissionLevel = requiredPermissionLevel,
            width = TIER_WIDTH.toFloat(),
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] + 1).coerceAtMost(25))
            }
        }

        registerButton(
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
            text = "-",
            requiredPermissionLevel = requiredPermissionLevel,
            width = TIER_WIDTH.toFloat(),
        ) {
            itemDistribution = itemDistribution.toMutableList().apply {
                set(i, (itemDistribution[i] - 1).coerceAtLeast(0))
            }
        }

        registerEntity(EntityType.TEXT_DISPLAY, position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
            putString("text", Text.Serialization.toJsonString(
                Text.literal(tier.name).formatted(tier.formatting, Formatting.BOLD)
            ))
            putString("billboard", "fixed")
            putString("alignment", "center")
            putInt("background", 0)
            putBoolean("shadow", true)
        }

        registerEntity(EntityType.TEXT_DISPLAY, position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0)) {
            val color = when {
                itemDistribution.sum() == 25 -> Formatting.WHITE
                itemDistribution.sum() < 25 -> Formatting.YELLOW
                else -> Formatting.RED
            }

            putString("text", Text.Serialization.toJsonString(
                Text.literal(itemDistribution[i].toString())
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
