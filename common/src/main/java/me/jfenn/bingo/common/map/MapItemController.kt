package me.jfenn.bingo.common.map

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService

class MapItemController(
    private val state: BingoState,
    private val config: BingoConfig,
    private val cardViewService: CardViewService,
    private val mapItemService: MapItemService,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    events: ScopedEvents,
) : BingoComponent() {
    private fun givePreviewCardItem(player: IPlayerHandle) {
        if (cardViewService.supportsCardHud(player)) return

        val hasMap = player.allHeldStacks()
            .any { mapItemService.isMapItem(it) }

        if (!hasMap) {
            val mapItem = mapItemService.createPreviewMapItem()
            player.giveItemStack(mapItem)
        }
    }

    private fun removePreviewCardItem(player: IPlayerHandle) {
        for ((slot, item) in player.allInventorySlots()) {
            if (mapItemService.isPreviewMapItem(item))
                player.removeStack(slot)
        }
    }

    /**
     * Ensure that the player has their team's bingo map card.
     * Has no effect if the player already has a card
     */
    private fun giveMapCardItem(player: IPlayerHandle) {
        if (cardViewService.supportsCardHud(player)) return

        val team = teamService.getPlayerTeam(player) ?: return
        val hasMap = player.allHeldStacks()
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
            player.giveItemStack(mapItem)
        }
    }

    /**
     * Ensure that the player has every team's bingo map card
     * in their hotbar slots. Indiscriminately replaces any existing
     * hotbar items.
     */
    private fun giveSpectatorMapItems(player: IPlayerHandle) {
        if (cardViewService.supportsCardHud(player)) return

        state.getRegisteredTeams()
            .forEachIndexed { i, team ->
                val hasMap = player.allHeldStacks()
                    .any { mapItemService.isMapTeamItem(it, team) }

                if (!hasMap) {
                    val stack = mapItemService.createMapItem(team)
                    if (state.isLobbyMode) {
                        // if isLobbyMode=true, then it's okay to replace hotbar items...
                        player.setStack(i, stack)
                    } else {
                        // otherwise, give the cards without overwriting their inventory
                        player.giveItemStack(stack)
                    }
                }
            }
    }

    /**
     * Remove all bingo map items from the player
     */
    private fun removeAllMapItems(player: IPlayerHandle) {
        player.allHeldStacks()
            .filter { mapItemService.isMapItem(it) }
            .forEach { it.count = 0 }
    }

    init {
        events.onEnter(GameState.POSTGAME) {
            for (player in playerManager.getPlayers()) {
                // remove any existing maps in the player's inventory
                removeAllMapItems(player)

                // give all maps for active teams to the player
                if (state.isLobbyMode && !cardViewService.supportsCardHud(player)) {
                    giveSpectatorMapItems(player)
                }
            }

            // if isLobbyMode=false, give the players a "memento card" after each game
            if (!state.isLobbyMode && config.giveMementoInSurvival) {
                for (player in playerManager.getPlayers()) {
                    val team = teamService.getPlayerTeam(player) ?: continue
                    val mapItem = mapItemService.createMementoMapItem(team)
                    player.giveItemStack(mapItem)
                }
            }
        }

        events.onGameTick {
            for (player in playerManager.getPlayers()) {
                val playerTeam = teamService.getPlayerTeam(player)
                val canHaveCards = player.isAlive && !player.isSpectator

                if (canHaveCards && cardViewService.isViewingPreviewCard(player)) {
                    givePreviewCardItem(player)
                } else {
                    removePreviewCardItem(player)
                }

                if (canHaveCards) {
                    if (cardViewService.isViewingSpectatorCards(player)) {
                        if (state.isLobbyMode)
                            giveSpectatorMapItems(player)
                    } else if (playerTeam != null && cardViewService.isViewingTeamCard(player, playerTeam)) {
                        // give player a new map, if they don't have one?
                        giveMapCardItem(player)
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