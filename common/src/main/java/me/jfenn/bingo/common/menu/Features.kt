package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoConfig
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerFeatures(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Features\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = "Unlock Recipes",
        toggleProp = propertyRef(config::isUnlockRecipes),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Keep Inventory",
        toggleProp = propertyRef(config::isKeepInventory),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = "Elytra",
        toggleProp = propertyRef(config::isElytra),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = "Allow PvP",
        toggleProp = propertyRef(config::isPvpEnabled),
    )
}