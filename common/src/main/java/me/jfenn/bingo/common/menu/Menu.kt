package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f

fun MenuComponent.registerMenu(
    title: Text,
    options: List<Text>,
    position: Vec3d,
    selectedIndexProp: MutableProperty<Int>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    text: TextProvider = koinScope.get(),
) {
    var selectedIndex by selectedIndexProp

    options.forEachIndexed { i, option ->
        registerButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i * MENU_ITEM_HEIGHT), 0.0),
            text = option,
            isActiveProp = computedProperty { selectedIndex == i },
            permissionGetter = permissionGetter,
        ) {
            selectedIndex = i
        }
    }

    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(title))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
        put("transformation", Matrix4f().scale(MENU_TEXT_SCALE).toNbt())
    }
}

inline fun <reified E : Enum<E>> MenuComponent.registerEnumMenu(
    position: Vec3d,
    title: Text,
    valueProp: MutableProperty<E>,
    valueText: (E) -> Text,
    noinline permissionGetter: (ServerPlayerEntity) -> Boolean,
) {
    val options = E::class.java.enumConstants

    var value by valueProp
    val indexProp = DelegatedProperty(
        getter = { options.indexOf(value) },
        setter = { value = options[it] },
    )

    registerMenu(
        title = title,
        options = options.map(valueText),
        selectedIndexProp = indexProp,
        position = position,
        permissionGetter = permissionGetter,
    )
}

fun MenuComponent.registerDimensionMenu(
    position: Vec3d,
    dimensionProperty: MutableProperty<String>,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    server: MinecraftServer = koinScope.get(),
    text: TextProvider = koinScope.get(),
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
        title = text.string(StringKey.OptionsSpawnDimension),
        options = dimensions.map { Text.literal(it.path.formatTitle()) },
        selectedIndexProp = indexProp,
        position = position,
        permissionGetter = permissionGetter,
    )
}
