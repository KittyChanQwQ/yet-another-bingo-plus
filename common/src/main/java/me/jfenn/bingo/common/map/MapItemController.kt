package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService

class MapItemController(
    private val state: BingoState,
    private val options: BingoOptions,
    private val cardViewService: CardViewService,
    private val mapItemService: MapItemService,
    private val mapRenderService: MapRenderService,
    private val mapService: IMapService,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    events: ScopedEvents,
) : BingoComponent() {
    private fun givePreviewCardItem(player: IPlayerHandle) {
        if (cardViewService.supportsCardHud(player.player)) return

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
        if (cardViewService.supportsCardHud(player.player)) return

        val team = teamService.getPlayerTeam(player.player) ?: return
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
        if (cardViewService.supportsCardHud(player.player)) return

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
                if (state.isLobbyMode && !cardViewService.supportsCardHud(player.player)) {
                    giveSpectatorMapItems(player)
                }
            }

            // if isLobbyMode=false, give the players a "memento card" after each game
            if (!state.isLobbyMode) {
                for (team in state.getRegisteredTeams()) {
                    // ensure that the map is drawn *without* any flashing/purple tiles
                    // (since the map is never redrawn after this event)
                    val map = team.getMap(mapService)
                    val view = map.view.copy(
                        tiles = map.view.tiles.map {
                            it.copy(isFlashing = false)
                        }.toMutableList(),
                    )
                    mapRenderService.update(team, map, view)

                    val mapItem = mapItemService.createMementoMapItem(team)
                    team.players
                        .mapNotNull { playerManager.getPlayer(it.uuid) }
                        .forEach { it.giveItemStack(mapItem.copy()) }
                }
            }
        }

        events.onGameTick {
            for (player in playerManager.getPlayers()) {
                val playerTeam = teamService.getPlayerTeam(player.player)

                if (player.isAlive && !player.isSpectator) {
                    if (state.state == GameState.PREGAME) {
                        if (cardViewService.isViewingPreviewCard()) {
                            givePreviewCardItem(player)
                        } else {
                            removePreviewCardItem(player)
                        }
                    } else if (playerTeam != null && cardViewService.isViewingTeamCard(player.player, playerTeam)) {
                        // give player a new map, if they don't have one?
                        giveMapCardItem(player)
                    } else if (cardViewService.isViewingSpectatorCards(player.player)) {
                        if (state.isLobbyMode)
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
                if (cardViewService.supportsCardHud(player.player))
                    removeAllMapItems(player)
            }
        }
    }
}