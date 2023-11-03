package me.jfenn.bingo.menu

import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerFeatures(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        putString("text", Text.Serializer.toJson(
            Text.literal("Features")
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = "Unlock Recipes",
        toggleProp = MutableProperty.ref(config::isUnlockRecipes),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Keep Inventory",
        toggleProp = MutableProperty.ref(config::isKeepInventory),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = "Elytra",
        toggleProp = MutableProperty.ref(config::isElytra),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = "Allow PvP",
        toggleProp = MutableProperty.ref(config::isPvpEnabled),
    )
}