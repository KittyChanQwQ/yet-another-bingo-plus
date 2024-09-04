package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerWinBehavior(
    position: Vector3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    options: BingoOptions = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsWinBehavior)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsWinBehaviorSkipCardWhenLost),
        toggleProp = propertyRef(options::skipCardWhenLost),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsWinBehaviorSkipCardWhenStalemate),
        toggleProp = propertyRef(options::skipCardWhenStalemate),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsWinBehaviorEndWhenComplete),
        toggleProp = propertyRef(options::endGameWhenComplete),
        permissionGetter = permissionGetter,
    )
}