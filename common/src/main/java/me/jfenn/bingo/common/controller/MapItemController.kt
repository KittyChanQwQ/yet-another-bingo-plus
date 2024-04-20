package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IItemStackFactory
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.common.utils.allInventorySlots
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

class MapItemController(
    private val server: MinecraftServer,
    private val state: BingoState,
    private val options: BingoOptions,
    private val cardViewService: CardViewService,
    private val mapItemService: MapItemService,
    private val itemStackFactory: IItemStackFactory,
    private val teamService: TeamService,
    events: ScopedEvents,
) : BingoComponent() {
    fun givePreviewCardItem(player: ServerPlayerEntity) {
        if (cardViewService.supportsCardHud(player)) return

        val hasMap = player.allHeldStacks()
            .map { itemStackFactory.forStack(it) }
            .any { mapItemService.isMapItem(it) }

        if (!hasMap) {
            val mapItem = mapItemService.createPreviewMapItem()
            player.giveItemStack(mapItem.stack)
        }
    }

    fun removePreviewCardItem(player: ServerPlayerEntity) {
        for ((slot, item) in player.allInventorySlots()) {
            if (mapItemService.isPreviewMapItem(itemStackFactory.forStack(item)))
                player.inventory.removeStack(slot)
        }
    }

    /**
     * Ensure that the player has their team's bingo map card.
     * Has no effect if the player already has a card
     */
    fun giveMapCardItem(player: ServerPlayerEntity) {
        if (cardViewService.supportsCardHud(player)) return

        val team = teamService.getPlayerTeam(player) ?: return
        val hasMap = player.allHeldStacks()
            .map { itemStackFactory.forStack(it) }
            .filter { mapItemService.isMapItem(it) }
            .mapNotNull {
                if (mapItemService.isMapTeamItem(it, team)) {
                    it.count = 1
                    it
                } else {
                    // if the player has another team's map item, remove it!
                    it.count = 0
                    null
                }
            }
            .toList()
            .isNotEmpty()

        if (!hasMap) {
            // give the player a new map item
            val mapItem = mapItemService.createMapItem(team)
            player.giveItemStack(mapItem.stack)
        }
    }

    /**
     * Ensure that the player has every team's bingo map card
     * in their hotbar slots. Indiscriminately replaces any existing
     * hotbar items.
     */
    fun giveSpectatorMapItems(player: ServerPlayerEntity) {
        if (cardViewService.supportsCardHud(player)) return

        state.getRegisteredTeams()
            .forEachIndexed { i, team ->
                val hasMap = player.allHeldStacks()
                    .map { itemStackFactory.forStack(it) }
                    .any { mapItemService.isMapTeamItem(it, team) }

                if (!hasMap) {
                    val stack = mapItemService.createMapItem(team)
                    player.getStackReference(i).set(stack.stack)
                }
            }
    }

    /**
     * Remove all bingo map items from the player
     */
    fun removeAllMapItems(player: ServerPlayerEntity) {
        player.allHeldStacks()
            .map { itemStackFactory.forStack(it) }
            .filter { mapItemService.isMapItem(it) }
            .forEach { it.count = 0 }
    }

    init {
        events.onEnter(GameState.POSTGAME) {
            for (player in server.playerManager.playerList) {
                // remove any existing maps in the player's inventory
                removeAllMapItems(player)

                // give all maps for active teams to the player
                giveSpectatorMapItems(player)
            }
        }

        events.onGameTick {
            for (player in server.playerManager.playerList) {
                val playerTeam = teamService.getPlayerTeam(player)

                if (player.isAlive && !player.isSpectator) {
                    if (state.state == GameState.PREGAME) {
                        if (cardViewService.isViewingPreviewCard()) {
                            givePreviewCardItem(player)
                        } else {
                            removePreviewCardItem(player)
                        }
                    } else if (playerTeam != null && cardViewService.isViewingTeamCard(player, playerTeam)) {
                        // give player a new map, if they don't have one?
                        giveMapCardItem(player)
                    } else if (cardViewService.isViewingSpectatorCards(player)) {
                        giveSpectatorMapItems(player)
                    }
                }

                if (state.state == GameState.PREGAME && player.isAlive) {
                    if (options.showPreviewCard) {
                        givePreviewCardItem(player)
                    } else {
                        removePreviewCardItem(player)
                    }
                }

                // If the player is using the client-side hud, make sure they don't have any map items
                // (this might be false when the player is first connecting)
                if (cardViewService.supportsCardHud(player))
                    removeAllMapItems(player)
            }
        }
    }
}