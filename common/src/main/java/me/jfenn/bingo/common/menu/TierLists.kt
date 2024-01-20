package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.TierListService
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTierLists(
    position: Vec3d,
    options: BingoOptions,
    onUpdate: () -> Unit,
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Tier Lists\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    TierListService.tierLists.forEachIndexed { i, listName ->
        val toggleProp = DelegatedProperty(
            getter = { options.tierLists.contains(listName) },
            setter = { isEnabled ->
                val lists = options.tierLists.filter { TierListService.tierLists.contains(it) }
                if (isEnabled) options.tierLists = lists + listName
                else options.tierLists = lists - listName
                markDirty()
                onUpdate()
            }
        )

        registerToggleButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = listName.formatTitle(),
            toggleProp = toggleProp,
        )
    }
}