package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.EndWhen
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
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
        tooltip = buildTooltip(StringKey.OptionsWinBehaviorSkipCardWhenLost),
        toggleProp = propertyRef(options::skipCardWhenLost),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsWinBehaviorEndWhenStalemate),
        tooltip = buildTooltip(StringKey.OptionsWinBehaviorEndWhenStalemate),
        toggleProp = propertyRef(options::endGameWhenStalemate),
        permissionGetter = permissionGetter,
    )

    val toggleEndWhen = arrayOf(EndWhen.FirstWin, EndWhen.AllWin, EndWhen.Never)
    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = Text.empty(),
        textProp = computedProperty {
            text.string(StringKey.OptionsEndWhen, options.endGameWhen.string(text))
        },
        tooltip = buildTooltip(StringKey.OptionsWinBehaviorEndWhen),
        permissionGetter = permissionGetter,
    ) {
        val next = toggleEndWhen
            .indexOfFirst { it::class == options.endGameWhen::class }
            .plus(1)
            .mod(toggleEndWhen.size)
            .coerceIn(toggleEndWhen.indices)

        options.endGameWhen = toggleEndWhen[next]
        markDirty()
    }
}