package me.jfenn.bingo.common.menu

import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.ITextDisplayEntity
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

const val MAX_SPAWN_DISTANCE = 500

fun MenuComponent.registerSpawnDistance(
    position: Vector3d,
    spawnDistanceProp: MutableProperty<Int>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    registerNumberInput(
        position = position,
        valueProp = spawnDistanceProp,
        minValueProp = ConstantProperty(0),
        maxValueProp = ConstantProperty(MAX_SPAWN_DISTANCE),
        step = 5,
        format = { text.string(StringKey.OptionsSpawnDistanceChunks, it) },
        tooltip = buildTooltip(StringKey.OptionsSpawnDistance),
    )

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsSpawnDistance)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }
}
