package me.jfenn.bingo.common.menu

import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerScoreInfo(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsScoreInfo)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = text.string(StringKey.OptionsRemainingTime),
        tooltip = buildTooltip(StringKey.OptionsRemainingTime),
        toggleProp = propertyRef(options::showRemainingTime),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsCompletedItems),
        tooltip = buildTooltip(StringKey.OptionsCompletedItems),
        toggleProp = propertyRef(options::showCompletedItems),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsCompletedLines),
        tooltip = buildTooltip(StringKey.OptionsCompletedLines),
        toggleProp = propertyRef(options::showCompletedLines),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsLeadingTeam),
        tooltip = buildTooltip(StringKey.OptionsLeadingTeam),
        toggleProp = propertyRef(options::showLeadingTeam),
        permissionGetter = permissionGetter,
    )
}