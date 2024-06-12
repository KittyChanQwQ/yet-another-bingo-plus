package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f

fun MenuComponent.registerScoreInfo(
    position: Vec3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(text.string(StringKey.OptionsScoreInfo)))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
        put("transformation", Matrix4f().scale(MENU_TEXT_SCALE).toNbt())
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = text.string(StringKey.OptionsRemainingTime),
        toggleProp = propertyRef(options::showRemainingTime),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsCompletedItems),
        toggleProp = propertyRef(options::showCompletedItems),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsCompletedLines),
        toggleProp = propertyRef(options::showCompletedLines),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsLeadingTeam),
        toggleProp = propertyRef(options::showLeadingTeam),
        permissionGetter = permissionGetter,
    )
}