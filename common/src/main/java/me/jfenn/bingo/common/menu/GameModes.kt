package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerGameModes(
    position: Vec3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
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
        toggleProp = propertyRef(options::isLockoutMode),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Inventory",
        toggleProp = propertyRef(options::isInventoryMode),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = "Ranked",
        toggleProp = propertyRef(options::isRankedMode),
        permissionGetter = permissionGetter,
    )
}