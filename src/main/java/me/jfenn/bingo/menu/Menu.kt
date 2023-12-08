package me.jfenn.bingo.menu

import me.jfenn.bingo.LOBBY_WORLD_ID
import net.minecraft.entity.EntityType
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerMenu(
    title: String,
    options: List<String>,
    position: Vec3d,
    selectedIndexProp: MutableProperty<Int>,
    requiredPermissionLevel: Int = 2,
) {

    var selectedIndex by selectedIndexProp

    options.forEachIndexed { i, option ->
        registerButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i * MENU_ITEM_HEIGHT), 0.0),
            text = option,
            isActiveProp = computedProperty { selectedIndex == i },
            requiredPermissionLevel = requiredPermissionLevel,
        ) {
            selectedIndex = i
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", Text.Serialization.toJsonString(
            Text.literal(title)
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}

inline fun <reified E : Enum<E>> MenuComponent.registerEnumMenu(
    position: Vec3d,
    title: String,
    valueProp: MutableProperty<E>,
) {
    val options = E::class.java.enumConstants

    var value by valueProp
    val indexProp = DelegatedProperty(
        getter = { options.indexOf(value) },
        setter = { value = options[it] },
    )

    registerMenu(
        title = title,
        options = options.map { it.name.replace('_', ' ') },
        selectedIndexProp = indexProp,
        position = position,
    )
}

fun MenuComponent.registerDimensionMenu(
    server: MinecraftServer,
    position: Vec3d,
    dimensionProperty: MutableProperty<String>,
) {
    val dimensions = server.worlds
        .map { it.registryKey.value }
        .filter { it != LOBBY_WORLD_ID }
        .sortedBy { it.toString() }

    var dimension by dimensionProperty
    val indexProp = DelegatedProperty(
        getter = { dimensions.indexOfFirst { it.toString() == dimension } },
        setter = { dimension = dimensions[it].toString() },
    )

    registerMenu(
        title = "Spawn Dimension",
        options = dimensions.map { it.path },
        selectedIndexProp = indexProp,
        position = position,
    )
}
