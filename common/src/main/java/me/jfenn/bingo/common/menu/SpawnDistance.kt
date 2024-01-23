package me.jfenn.bingo.common.menu

import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

const val MAX_SPAWN_DISTANCE = 500

fun MenuComponent.registerSpawnDistance(
    position: Vec3d,
    spawnDistanceProp: MutableProperty<Int>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
) {
    var spawnDistance by spawnDistanceProp

    registerButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        permissionGetter = permissionGetter,
    ) {
        spawnDistance = (spawnDistance + 10).coerceIn(0, MAX_SPAWN_DISTANCE)
        markDirty()
    }

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        permissionGetter = permissionGetter,
    ) {
        spawnDistance = (spawnDistance - 10).coerceIn(0, MAX_SPAWN_DISTANCE)
        markDirty()
    }

    registerEntity(EntityType.TEXT_DISPLAY, position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0)) {
        putString("text", "\"$spawnDistance chunks\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Spawn Distance\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
