package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.controller.GameRuleController
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerFeatures(
    position: Vec3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
    gameRuleController: GameRuleController = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(text.string(StringKey.OptionsFeatures)))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = text.string(StringKey.OptionsUnlockRecipes),
        toggleProp = propertyRef(options::isUnlockRecipes),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsKeepInventory),
        toggleProp = propertyRef(options::isKeepInventory),
        permissionGetter = permissionGetter,
    ) {
        gameRuleController.writeToServer(it.server, options)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsElytra),
        toggleProp = propertyRef(options::isElytra),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsNightVis),
        toggleProp = propertyRef(options::isNightVision),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 4*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsAllowPvp),
        toggleProp = propertyRef(options::isPvpEnabled),
        permissionGetter = permissionGetter,
    ) {
        gameRuleController.writeToServer(it.server, options)
    }
}