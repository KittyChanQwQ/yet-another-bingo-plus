package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.TierListService
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerTierLists(
    position: Vec3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    cardService: CardService = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Tier List\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    TierListService.tierLists.forEachIndexed { i, listName ->
        registerButton(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i*MENU_ITEM_HEIGHT), 0.0),
            text = listName.formatTitle(),
            isActiveProp = computedProperty { options.tierList == listName },
            permissionGetter = permissionGetter,
        ) {
            options.tierList = listName
            markDirty()

            // when changed, update the bingo card
            cardService.generateCard()
        }
    }
}