package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f

const val MAX_SPAWN_DISTANCE = 500

fun MenuComponent.registerSpawnDistance(
    position: Vec3d,
    spawnDistanceProp: MutableProperty<Int>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    registerNumberInput(
        position = position,
        valueProp = spawnDistanceProp,
        minValueProp = ConstantProperty(0),
        maxValueProp = ConstantProperty(MAX_SPAWN_DISTANCE),
        step = 10,
        format = { text.string(StringKey.OptionsSpawnDistanceChunks, it) },
        permissionGetter = permissionGetter,
    )

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(text.string(StringKey.OptionsSpawnDistance)))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
        put("transformation", Matrix4f().scale(MENU_TEXT_SCALE).toNbt())
    }
}
