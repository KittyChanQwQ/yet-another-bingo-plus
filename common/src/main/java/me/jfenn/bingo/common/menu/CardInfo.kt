package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerCardInfo(
    position: Vec3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    options: BingoOptions = koinScope.get(),
    cardService: CardService = koinScope.get(),
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
        toggleProp = propertyRef(options::showOtherTeamCompletions),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Hidden Items",
        toggleProp = propertyRef(options::hiddenItems),
        permissionGetter = permissionGetter,
    ) {
        // when changed, re-roll the bingo card
        if (options.hiddenItems)
            cardService.shuffleCard()
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT*2), 0.0),
        text = "Preview Card",
        toggleProp = propertyRef(options::showPreviewCard),
        permissionGetter = permissionGetter,
    ) {
        // when changed, re-roll the bingo card
        if (!options.showPreviewCard)
            cardService.shuffleCard()
    }
}