package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerGameModes(
    position: Vector3d,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    state: BingoState = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    val cardOptions by DelegatedProperty(
        getter = { state.getActiveCard().options },
        setter = {},
    )

    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsGameMode)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsModeLockout),
        tooltip = buildTooltip(StringKey.OptionsModeLockout),
        toggleProp = DelegatedProperty(
            getter = {
                cardOptions.isLockoutMode
            },
            setter = {
                cardOptions.isLockoutMode = it
                markDirty()
            }
        ),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsModeInventory),
        tooltip = buildTooltip(StringKey.OptionsModeInventory),
        toggleProp = DelegatedProperty(
            getter = {
                cardOptions.isInventoryMode
            },
            setter = {
                cardOptions.isInventoryMode = it
                markDirty()
            }
        ),
        permissionGetter = permissionGetter,
    )

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0, Vector3d()),
        text = text.string(StringKey.OptionsModeHiddenItems),
        tooltip = buildTooltip(StringKey.OptionsModeHiddenItems),
        toggleProp = DelegatedProperty(
            getter = {
                cardOptions.isHiddenItemsMode
            },
            setter = {
                cardOptions.isHiddenItemsMode = it
                markDirty()
            }
        ),
        permissionGetter = permissionGetter,
    ) {
        cardService.generateCard()
    }
}