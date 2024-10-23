package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.EntityType
import me.jfenn.bingo.api.ITextDisplayEntity
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.spawn.SpawnItem
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import org.joml.Matrix4f
import org.joml.Vector3d

fun MenuComponent.registerSpawnKit(
    position: Vector3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
    cardService: CardService = koinScope.get(),
    text: TextProvider = koinScope.get(),
    itemStackFactory: IItemStackFactory = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        pos = position
        this.value = text.string(StringKey.OptionsSpawnKit)
        billboard = ITextDisplayEntity.Billboard.FIXED
        alignment = ITextDisplayEntity.TextAlignment.CENTER
        background = 0
        shadow = true
        transformation = Matrix4f().scale(MENU_TEXT_SCALE)
    }

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = text.string(StringKey.OptionsSpawnKitPlayer),
        toggleProp = propertyRef(options::isPlayerKit),
        permissionGetter = permissionGetter,
    ) {
        cardService.generateCard()
    }

    registerButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = text.string(StringKey.OptionsSpawnKitEdit),
        permissionGetter = permissionGetter,
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

    registerToggleButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 3), 0.0),
        text = text.string(StringKey.OptionsSpawnKitTeam),
        toggleProp = propertyRef(options::isTeamKit),
        permissionGetter = permissionGetter,
    ) {
        cardService.generateCard()
    }

    registerButton(
        position = position + Vector3d(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 4), 0.0),
        text = text.string(StringKey.OptionsSpawnKitEdit),
        permissionGetter = permissionGetter,
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