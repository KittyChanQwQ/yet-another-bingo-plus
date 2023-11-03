package me.jfenn.bingo.menu

import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerMenu(
    title: String,
    options: List<String>,
    position: Vec3d,
    selectedIndexProp: MutableProperty<Int> = MutableProperty(0),
    requiredPermissionLevel: Int = 2,
) {

    var selectedIndex by property(selectedIndexProp)

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

    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        putString("text", Text.Serializer.toJson(
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

    val indexProp = MutableProperty(options.indexOf(value))
    indexProp.subscribe { value = options[it] }

    registerMenu(
        title = title,
        options = options.map { it.name.replace('_', ' ') },
        selectedIndexProp = indexProp,
        position = position,
    )
}

fun MenuComponent.registerDimensionMenu(
    position: Vec3d,
    dimensionProperty: MutableProperty<String>,
) {
    val dimensions = minecraftServer.worlds
        .filter { it != lobbyWorld }
        .map { it.registryKey.value }

    var dimension by dimensionProperty

    val indexProp = MutableProperty(dimensions.indexOfFirst { it.toString() == dimension })
    indexProp.subscribe { dimension = dimensions[it].toString() }

    registerMenu(
        title = "Spawn Dimension",
        options = dimensions.map { it.path },
        selectedIndexProp = indexProp,
        position = position,
    )
}
