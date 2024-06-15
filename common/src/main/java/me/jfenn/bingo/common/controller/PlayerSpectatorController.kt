package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spectator.SpectatorInventoryScreenHandler
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.block.*
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.ChestMinecartEntity
import net.minecraft.inventory.DoubleInventory
import net.minecraft.inventory.Inventory
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.world.GameMode

class PlayerSpectatorController(
    events: ScopedEvents,
    private val state: BingoState,
    private val teamService: TeamService,
    private val permissions: PermissionsIntegration,
) : BingoComponent() {

    init {
        events.onUseBlock {
            if (state.isLobbyMode) return@onUseBlock null
            val player = it.player as? ServerPlayerEntity ?: return@onUseBlock null
            if (state.state != GameState.PLAYING) return@onUseBlock null
            if (teamService.isPlaying(player)) return@onUseBlock null
            if (player.interactionManager.gameMode != GameMode.ADVENTURE) return@onUseBlock null

            // if the player is spectating a game, do not
            // allow them to modify any inventory

            val blockState = player.world.getBlockState(it.hit.blockPos)
            val block = blockState.block

            // if the player has permission to open doors...
            if (permissions.hasPermission(player, Permission.SPECTATOR_USE_DOORS)) {
                when (block) {
                    is DoorBlock -> return@onUseBlock null
                    is TrapdoorBlock -> return@onUseBlock null
                }
            }

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
            }

            if (
                permissions.hasPermission(player, Permission.SPECTATOR_VIEW_INVENTORY)
                && inventory != null
            ) {
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

            ActionResult.FAIL
        }

        events.onUseEntity {
            if (state.isLobbyMode) return@onUseEntity null
            val player = it.player as? ServerPlayerEntity ?: return@onUseEntity null
            if (state.state != GameState.PLAYING) return@onUseEntity null
            if (teamService.isPlaying(player)) return@onUseEntity null
            if (player.interactionManager.gameMode != GameMode.ADVENTURE) return@onUseEntity null

            // if the player is spectating a game, do not
            // allow them to modify any inventory

            // if the interacted entity has an inventory, open it as read-only...
            val inventory: Inventory? = when (it.entity) {
                is ChestMinecartEntity -> it.entity
                is ChestBoatEntity -> it.entity
                else -> null
            }

            if (
                permissions.hasPermission(player, Permission.SPECTATOR_VIEW_INVENTORY)
                && inventory != null
            ) {
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

            ActionResult.FAIL
        }

        events.onAttackEntity {
            if (state.isLobbyMode) return@onAttackEntity null
            val player = it.player as? ServerPlayerEntity ?: return@onAttackEntity null
            if (state.state != GameState.PLAYING) return@onAttackEntity null
            if (teamService.isPlaying(player)) return@onAttackEntity null
            if (player.interactionManager.gameMode != GameMode.ADVENTURE) return@onAttackEntity null

            // if the player is spectating a game, do not
            // allow them to attack any entity

            ActionResult.FAIL
        }
    }

}