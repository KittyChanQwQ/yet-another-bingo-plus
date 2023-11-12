package me.jfenn.bingo.menu

import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.TierListService
import me.jfenn.bingo.utils.formatTitle
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTierLists(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        putString("text", Text.Serializer.toJson(
            Text.literal("Tier Lists")
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    TierListService.tierLists.forEachIndexed { i, tierlist ->
        val toggleProp = DelegatedProperty(
            getter = { config.tierLists.contains(tierlist) },
            setter = {
                if (it) config.tierLists += tierlist
                else config.tierLists -= tierlist
                markDirty()
            }
        )

        registerToggleButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = tierlist.formatTitle(),
            toggleProp = toggleProp,
        )
    }
}