package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerScoreInfo(
    position: Vec3d,
    options: BingoOptions,
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
        toggleProp = propertyRef(options::showRemainingTime),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Completed Items",
        toggleProp = propertyRef(options::showCompletedItems),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
        text = "Completed Lines",
        toggleProp = propertyRef(options::showCompletedLines),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
        text = "Leading Team",
        toggleProp = propertyRef(options::showLeadingTeam),
    )
}