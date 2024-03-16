package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerCardInfo(
    position: Vec3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    options: BingoOptions = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(text.string(StringKey.OptionsBingoCard)))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = text.string(StringKey.OptionsShowCompletions),
        toggleProp = propertyRef(options::showOtherTeamCompletions),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsPreviewCard),
        toggleProp = propertyRef(options::showPreviewCard),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT*2), 0.0),
        text = text.string(StringKey.OptionsShowHiddenTiers),
        toggleProp = propertyRef(options::showHiddenTiers),
        permissionGetter = permissionGetter,
    ) {
        cardService.generateCard()
    }
}