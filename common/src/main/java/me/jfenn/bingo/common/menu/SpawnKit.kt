package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.spawn.SpawnItem
import net.minecraft.entity.EntityType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerSpawnKit(
    position: Vec3d,
    options: BingoOptions,
    permissionGetter: (ServerPlayerEntity) -> Boolean,
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", "\"Spawn Kit\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = "Player Items",
        toggleProp = propertyRef(options::isPlayerKit),
        permissionGetter = permissionGetter,
    )

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Edit...",
        permissionGetter = permissionGetter,
    ) { player ->
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, inv, _ ->
                    InventoryScreenHandler(syncId, options.playerKitItems.flatMap { it.toItemStacks() }, inv).apply {
                        onClose { stacks ->
                            options.playerKitItems = stacks.map { SpawnItem.fromItemStack(it) }
                        }
                    }
                },
                Text.literal("Player Items")
            )
        )
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 3), 0.0),
        text = "Team Chest",
        toggleProp = propertyRef(options::isTeamKit),
        permissionGetter = permissionGetter,
    )

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 4), 0.0),
        text = "Edit...",
        permissionGetter = permissionGetter,
    ) { player ->
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, inv, _ ->
                    InventoryScreenHandler(syncId, options.teamKitItems.flatMap { it.toItemStacks() }, inv).apply {
                        onClose { stacks ->
                            options.teamKitItems = stacks.map { SpawnItem.fromItemStack(it) }
                        }
                    }
                },
                Text.literal("Team Chest")
            )
        )
    }
}