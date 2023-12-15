package me.jfenn.bingo.menu

import me.jfenn.bingo.config.BingoConfig
import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerScoreInfo(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Score Info\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = "Remaining Time",
        toggleProp = propertyRef(config::showRemainingTime),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Completed Items",
        toggleProp = propertyRef(config::showCompletedItems),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = "Completed Lines",
        toggleProp = propertyRef(config::showCompletedLines),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = "Leading Team",
        toggleProp = propertyRef(config::showLeadingTeam),
    )
}