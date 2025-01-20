package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.options.OptionsService
import me.jfenn.bingo.common.spawn.SpawnItem
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.util.Formatting
import org.joml.Vector3d

internal const val MENU_SPAWN_KIT_WIDTH = 2.0
private const val EDIT_HEIGHT = 0.55

internal fun MenuComponent.registerSpawnKit(
    position: Vector3d,
    state: BingoState = koinScope.get(),
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
    itemStackFactory: IItemStackFactory = koinScope.get(),
    optionsService: OptionsService = koinScope.get(),
) {
    val options by state::options
    val offset = Vector3d()

    registerTitlePanel(
        position = position + offset.sub(0.0, 2*MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = MENU_SPAWN_KIT_WIDTH,
        title = text.string(StringKey.OptionsSpawnKit),
    )

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = MENU_SPAWN_KIT_WIDTH,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsSpawnKitPlayer),
        tooltip = buildTooltip(StringKey.OptionsSpawnKitPlayer),
        isActiveProp = computedProperty { options.isPlayerKit },
    ) {
        options.isPlayerKit = !options.isPlayerKit
        cardService.generateCard()
        optionsService.broadcastHotbarMessage(
            it,
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsSpawnKitPlayer,
                text.boolean(options.isPlayerKit)
            )
        )
    }

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + EDIT_HEIGHT, 0.0),
        width = MENU_SPAWN_KIT_WIDTH,
        height = EDIT_HEIGHT,
        icon = "\uD83C\uDF71",
        text = text.string(StringKey.OptionsSpawnKitEdit),
        tooltip = listOf(
            text.string(StringKey.OptionsSpawnKitPlayerEdit).formatted(Formatting.GREEN),
            text.string(StringKey.OptionsSpawnKitEditTooltip)
        ),
        blockIdentifier = MENU_BUTTON_ALT_MATERIAL,
    ) { player ->
        player.player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, inv, _ ->
                    InventoryScreenHandler(syncId, options.playerKitItems.flatMap { it.toItemStacks(itemStackFactory) }, inv).apply {
                        onClose { stacks ->
                            options.playerKitItems = stacks
                                .map { itemStackFactory.forStack(it) }
                                .map { SpawnItem.fromItemStack(it) }

                            cardService.generateCard()
                        }
                    }
                },
                text.string(StringKey.OptionsSpawnKitPlayer)
            )
        )
    }

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + MENU_LINE_HEIGHT, 0.0),
        width = MENU_SPAWN_KIT_WIDTH,
        height = MENU_LINE_HEIGHT,
        text = text.string(StringKey.OptionsSpawnKitTeam),
        tooltip = buildTooltip(StringKey.OptionsSpawnKitTeam),
        isActiveProp = computedProperty { options.isTeamKit },
    ) {
        options.isTeamKit = !options.isTeamKit
        cardService.generateCard()
        optionsService.broadcastHotbarMessage(
            it,
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsSpawnKitTeam,
                text.boolean(options.isTeamKit)
            )
        )
    }

    registerTileButton(
        position = position + offset.sub(0.0, MENU_LINE_PADDING + EDIT_HEIGHT, 0.0),
        width = MENU_SPAWN_KIT_WIDTH,
        height = EDIT_HEIGHT,
        icon = "\uD83C\uDF71",
        text = text.string(StringKey.OptionsSpawnKitEdit),
        tooltip = listOf(
            text.string(StringKey.OptionsSpawnKitTeamEdit).formatted(Formatting.GREEN),
            text.string(StringKey.OptionsSpawnKitEditTooltip)
        ),
        blockIdentifier = MENU_BUTTON_ALT_MATERIAL,
    ) { player ->
        player.player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, inv, _ ->
                    InventoryScreenHandler(syncId, options.teamKitItems.flatMap { it.toItemStacks(itemStackFactory) }, inv).apply {
                        onClose { stacks ->
                            options.teamKitItems = stacks
                                .map { itemStackFactory.forStack(it) }
                                .map { SpawnItem.fromItemStack(it) }

                            cardService.generateCard()
                        }
                    }
                },
                text.string(StringKey.OptionsSpawnKitTeam)
            )
        )
    }
}