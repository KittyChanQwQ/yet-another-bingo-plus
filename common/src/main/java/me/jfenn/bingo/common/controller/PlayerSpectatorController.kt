package me.jfenn.bingo.common.controller

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IChestBoatEntity
import me.jfenn.bingo.platform.IChestMinecartEntity
import me.jfenn.bingo.platform.PlayerGameMode
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ActionResult
import me.jfenn.bingo.platform.event.model.AttackEntityEvent
import me.jfenn.bingo.platform.event.model.UseBlockEvent
import me.jfenn.bingo.platform.event.model.UseEntityEvent
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.player.IContainerMenu

internal class PlayerSpectatorController(
    eventBus: IEventBus,
    private val state: BingoState,
    private val teamService: TeamService,
    private val permissions: IPermissionsApi,
    private val containerMenuFactory: IContainerMenu.Factory,
) : BingoComponent() {

    init {
        eventBus.register(UseBlockEvent) {
            if (!state.isLobbyMode) return@register null
            val player = it.player
            if (state.state != GameState.PLAYING) return@register null
            if (teamService.isPlaying(player)) return@register null
            if (player.gameMode != PlayerGameMode.ADVENTURE) return@register null

            // if the player is spectating a game, do not
            // allow them to modify any inventory

            val blockState = player.world.getBlockState(it.hit.blockPos)
            val block = blockState.block

            // if the player has permission to open doors...
            if (permissions.hasPermission(player, Permission.SPECTATOR_USE_DOORS) && block.isDoorBlock) {
                return@register null
            }

            // if the interacted block has an inventory, open it as read-only...
            val inventory = blockState.getInventory(player.world, it.hit.blockPos)

            if (
                permissions.hasPermission(player, Permission.SPECTATOR_VIEW_INVENTORY)
                && inventory != null
            ) {
                player.openMenu(
                    containerMenuFactory.createSpectatorInventory(inventory, block.name)
                )
            }

            ActionResult.Fail(Unit)
        }

        eventBus.register(UseEntityEvent) {
            if (!state.isLobbyMode) return@register null
            val player = it.player
            if (state.state != GameState.PLAYING) return@register null
            if (teamService.isPlaying(player)) return@register null
            if (player.gameMode != PlayerGameMode.ADVENTURE) return@register null

            // if the player is spectating a game, do not
            // allow them to modify any inventory

            // if the interacted entity has an inventory, open it as read-only...
            val inventory: IInventory? = when (
                val entity = it.entity
            ) {
                is IChestMinecartEntity -> entity.inventory
                is IChestBoatEntity -> entity.inventory
                else -> null
            }

            if (
                permissions.hasPermission(player, Permission.SPECTATOR_VIEW_INVENTORY)
                && inventory != null
            ) {
                player.openMenu(
                    containerMenuFactory.createSpectatorInventory(inventory, it.entity.name)
                )
            }

            ActionResult.Fail(Unit)
        }

        eventBus.register(AttackEntityEvent) {
            if (!state.isLobbyMode) return@register null
            val player = it.player
            if (state.state != GameState.PLAYING) return@register null
            if (teamService.isPlaying(player)) return@register null
            if (player.gameMode != PlayerGameMode.ADVENTURE) return@register null

            // if the player is spectating a game, do not
            // allow them to attack any entity

            ActionResult.Fail(Unit)
        }
    }

}