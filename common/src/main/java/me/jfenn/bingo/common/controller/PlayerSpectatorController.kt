package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.spectator.SpectatorInventoryScreenHandler
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.block.BarrelBlock
import net.minecraft.block.ChestBlock
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.ChestMinecartEntity
import net.minecraft.inventory.DoubleInventory
import net.minecraft.inventory.Inventory
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.TypedActionResult

class PlayerSpectatorController(
    events: ScopedEvents,
    private val state: BingoState,
    private val permissions: PermissionsIntegration,
) {

    init {
        events.onUseBlock {
            val player = it.player as? ServerPlayerEntity ?: return@onUseBlock null

            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (
                state.state == GameState.PLAYING
                && player.isCreative
                && !permissions.hasPermission(player, Permission.SPECTATOR_INTERACT_BLOCK)
            ) {
                val blockState = player.world.getBlockState(it.hit.blockPos)
                val block = blockState.block

                // if the interacted block has an inventory, open it as read-only...
                var rows = 0
                val inventory: Inventory? = when (block) {
                    is ChestBlock -> { // includes TrappedChestBlock and double-chests
                        val inventory = ChestBlock.getInventory(block, blockState, player.world, it.hit.blockPos, true)
                        rows = if (inventory is DoubleInventory) 6 else 3
                        inventory
                    }
                    is BarrelBlock -> {
                        rows = 3
                        player.world.getBlockEntity(it.hit.blockPos) as? Inventory
                    }
                    is ShulkerBoxBlock -> {
                        rows = 3
                        player.world.getBlockEntity(it.hit.blockPos) as? Inventory
                    }
                    else -> null
                }.takeIf {
                    permissions.hasPermission(player, Permission.SPECTATOR_VIEW_INVENTORY)
                }

                if (inventory != null) {
                    player.openHandledScreen(
                        SimpleNamedScreenHandlerFactory(
                            { syncId, inv, _ ->
                                SpectatorInventoryScreenHandler(
                                    type = when (rows) {
                                        6 -> ScreenHandlerType.GENERIC_9X6
                                        else -> ScreenHandlerType.GENERIC_9X3
                                    },
                                    syncId = syncId,
                                    rows = rows,
                                    playerInventory = inv,
                                    inventory = inventory,
                                )
                            },
                            block.name,
                        )
                    )
                }

                return@onUseBlock ActionResult.FAIL
            }
            null
        }

        events.onAttackBlock {
            val player = it.player as? ServerPlayerEntity ?: return@onAttackBlock null

            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (
                state.state == GameState.PLAYING
                && player.isCreative
                && !permissions.hasPermission(player, Permission.SPECTATOR_MODIFY_BLOCK)
            ) {
                return@onAttackBlock ActionResult.FAIL
            }

            null
        }

        events.onUseEntity {
            val player = it.player as? ServerPlayerEntity ?: return@onUseEntity null

            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (
                state.state == GameState.PLAYING
                && player.isCreative
                && !permissions.hasPermission(player, Permission.SPECTATOR_INTERACT_ENTITY)
            ) {
                // if the interacted entity has an inventory, open it as read-only...
                val inventory: Inventory? = when (it.entity) {
                    is ChestMinecartEntity -> it.entity
                    is ChestBoatEntity -> it.entity
                    else -> null
                }.takeIf {
                    permissions.hasPermission(player, Permission.SPECTATOR_VIEW_INVENTORY)
                }

                if (inventory != null) {
                    player.openHandledScreen(
                        SimpleNamedScreenHandlerFactory(
                            { syncId, inv, _ ->
                                SpectatorInventoryScreenHandler(
                                    type = ScreenHandlerType.GENERIC_9X3,
                                    syncId = syncId,
                                    rows = 3,
                                    playerInventory = inv,
                                    inventory = inventory,
                                )
                            },
                            it.entity.name,
                        )
                    )
                }

                return@onUseEntity ActionResult.FAIL
            }

            null
        }

        events.onAttackEntity {
            val player = it.player as? ServerPlayerEntity ?: return@onAttackEntity null

            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (
                state.state == GameState.PLAYING
                && player.isCreative
                && !permissions.hasPermission(player, Permission.SPECTATOR_MODIFY_ENTITY)
            ) {
                return@onAttackEntity ActionResult.FAIL
            }

            null
        }

        events.onUseItem {
            val player = it.player as? ServerPlayerEntity ?: return@onUseItem null

            // if the player enters creative mode during a game,
            // prevent them from modifying the world
            if (
                state.state == GameState.PLAYING
                && player.isCreative
                && !permissions.hasPermission(player, Permission.SPECTATOR_USE_ITEM)
            ) {
                return@onUseItem TypedActionResult.fail(null)
            }

            null
        }
    }

}