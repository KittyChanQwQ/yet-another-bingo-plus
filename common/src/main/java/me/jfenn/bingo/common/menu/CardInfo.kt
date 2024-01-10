package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.integrations.BingoCardController
import me.jfenn.bingo.common.state.BingoContext
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerCardInfo(
    position: Vec3d,
    ctx: BingoContext,
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
        toggleProp = propertyRef(ctx.config::showOtherTeamCompletions),
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Hidden Items",
        toggleProp = propertyRef(ctx.config::hiddenItems),
    ) {
        // when changed, re-roll the bingo card
        BingoCardController.shuffleCard(ctx)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT*2), 0.0),
        text = "Preview Card",
        toggleProp = propertyRef(ctx.config::showPreviewCard),
    )
}