package me.jfenn.bingo.menu

import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.spawn.SpawnItem
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerSpawnKit(
    position: Vec3d,
    config: BingoConfig,
) {
    registerEntity(EntityType.TEXT_DISPLAY) {
        put("Pos", position.toNbt())
        putString(
            "text", Text.Serializer.toJson(
                Text.literal("Spawn Kit")
            )
        )
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    registerToggleButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
        text = "Player Items",
        toggleProp = MutableProperty.ref(config::isPlayerKit)
    )

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
        text = "Edit...",
    ) { player ->
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, inv, _ ->
                    InventoryScreenHandler(syncId, config.playerKitItems.flatMap { it.toItemStacks() }, inv).apply {
                        onClose { stacks ->
                            config.playerKitItems = stacks.map { SpawnItem.fromItemStack(it) }
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
        toggleProp = MutableProperty.ref(config::isTeamKit)
    )

    registerButton(
        position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 4), 0.0),
        text = "Edit...",
    ) { player ->
        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
                { syncId, inv, _ ->
                    InventoryScreenHandler(syncId, config.teamKitItems.flatMap { it.toItemStacks() }, inv).apply {
                        onClose { stacks ->
                            config.teamKitItems = stacks.map { SpawnItem.fromItemStack(it) }
                        }
                    }
                },
                Text.literal("Team Chest")
            )
        )
    }
}