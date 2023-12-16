package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoConfig
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerGameModes(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Game Mode\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = "Lockout",
        toggleProp = propertyRef(config::isLockoutMode),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Inventory",
        toggleProp = propertyRef(config::isInventoryMode),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = "Ranked",
        toggleProp = propertyRef(config::isRankedMode),
    )
}