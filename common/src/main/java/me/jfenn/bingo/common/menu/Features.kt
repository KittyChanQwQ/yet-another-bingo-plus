package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.controller.GameRuleController
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerFeatures(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
    gameRuleController: GameRuleController = koinScope.get(),
    server: MinecraftServer = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsFeatures)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsPreviewCard),
        tooltip = buildTooltip(StringKey.OptionsPreviewCard),
        toggleProp = propertyRef(options::showPreviewCard),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsKeepInventory),
        toggleProp = propertyRef(options::isKeepInventory),
        permissionGetter = permissionGetter,
    ) {
        gameRuleController.writeToServer(server, options)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsElytra),
        toggleProp = propertyRef(options::isElytra),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsNightVis),
        toggleProp = propertyRef(options::isNightVision),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 4*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsAllowPvp),
        toggleProp = propertyRef(options::isPvpEnabled),
        permissionGetter = permissionGetter,
    ) {
        gameRuleController.writeToServer(server, options)
    }
}