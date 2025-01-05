package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.platform.IPlayerManager
import net.minecraft.server.MinecraftServer

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
class BingoMapController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val cardService: CardService,
    private val cardViewService: CardViewService,
    private val cardImageService: CardImageService,
) : BingoComponent() {

    init {
        events.onGameTick {
            cardImageService.sendNecessaryImages()

            // update all card map images
            if (state.state == GameState.PREGAME) {
                // if in pregame, update the preview card!
                cardViewService.updateCard(null)
            }

            for (team in state.getRegisteredTeams()) {
                cardViewService.updateCard(team)
            }
        }

        events.onUpdateTick {
            // Periodically send all valid cards to all players
            // to fix any missed events...
            for (player in playerManager.getPlayers()) {
                cardViewService.sendUpdatePackets(player)
            }
        }

        events.onPlayerJoin { player ->
            cardImageService.clearPlayerState(player.uuid)
        }

        events.onPlayerChannelRegister { player ->
            cardViewService.sendUpdatePackets(player)
        }

        events.onEnter(GameState.PREGAME) {
            for (card in state.cards) {
                cardService.generateCard(card)
            }
            for (player in server.playerManager.playerList) {
                cardViewService.sendClearDisplayPacket(player)
            }
        }

        events.onEnter(GameState.POSTGAME) {
            for (team in state.getRegisteredTeams()) {
                cardViewService.updateCard(team)
            }
        }
    }
}