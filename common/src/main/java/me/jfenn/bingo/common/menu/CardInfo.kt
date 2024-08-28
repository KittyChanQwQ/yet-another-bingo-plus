package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerCardInfo(
    position: Vector3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    options: BingoOptions = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsBingoCard)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0, Vector3d()),
        text = text.string(StringKey.OptionsShowCompletions),
        tooltip = buildList {
            add(text.string(StringKey.OptionsShowCompletions).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsShowCompletionsTooltip))
        },
        toggleProp = propertyRef(options::showOtherTeamCompletions),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsPreviewCard),
        tooltip = buildList {
            add(text.string(StringKey.OptionsPreviewCard).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsPreviewCardTooltip))
        },
        toggleProp = propertyRef(options::showPreviewCard),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT*2), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsShowHiddenTiers),
        tooltip = buildList {
            add(text.string(StringKey.OptionsShowHiddenTiers).formatted(Formatting.GREEN))
            add(text.string(StringKey.OptionsShowHiddenTiersTooltip))
        },
        toggleProp = propertyRef(options::showHiddenTiers),
        permissionGetter = permissionGetter,
    ) {
        cardService.generateCard()
    }
}