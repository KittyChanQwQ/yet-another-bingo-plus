package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoConfig
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerCardInfo(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"BINGO Card\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "Team Outlines",
        toggleProp = propertyRef(config::drawOtherTeamCompletions),
    )
}