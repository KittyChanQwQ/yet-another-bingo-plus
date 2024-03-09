package me.jfenn.bingo.common.map

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.PACKET_CARD_DISPLAY
import me.jfenn.bingo.common.PACKET_CARD_OPEN
import me.jfenn.bingo.common.PACKET_CARD_UPDATE
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
class BingoMapController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val mapRenderService: MapRenderService,
    private val cardService: CardService,
    private val cardViewService: CardViewService,
    private val advancementManager: IAdvancementManager,
    private val textSerializer: ITextSerializer,
) : BingoComponent() {

    fun updateCard(team: BingoTeam?) {
        val view = cardViewService.getMapView(team)
        val map = team?.getMap(server) ?: state.getPreviewMap(server)

        if (cardViewService.needsUpdate(map, view)) {
            mapRenderService.update(team, map, view)

            val packetBuf = view.toPacketBuf()
            for (player in server.playerManager.playerList) {
                if (cardViewService.isViewingCard(player, team) && cardViewService.supportsCardHud(player)) {
                    ServerPlayNetworking.send(player, PACKET_CARD_UPDATE, packetBuf)
                }
            }

            map.updatedAt = Clock.System.now()
            map.view = view
        }
    }

    init {
        // When the player opens the card with a keybind, show the inventory card view
        ServerPlayNetworking.registerGlobalReceiver(PACKET_CARD_OPEN) { server, player, handler, packet, response ->
            server.execute {
                cardService.showInventory(player)
            }
        }

        events.onGameTick {
            // first populate advancement images, if missing...
            for (entry in state.card.advancementEntries) {
                val advancement = advancementManager.getAdvancement(server, entry.identifierInstance)
                    ?: continue

                // If the entry doesn't have a visualItem yet, set it to match the advancement
                if (entry.visualItem == null) {
                    entry.visualItem = advancement.displayIcon ?: ItemStack.EMPTY
                    entry.name = advancement.name
                        ?.let { Text.empty()
                            .append(it).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GREEN)) }
                        ?.let { textSerializer.toJson(it) }
                    entry.description = advancement.description?.let { textSerializer.toJson(it) }
                }
            }

            // update all card map images
            if (state.state == GameState.PREGAME) {
                // if in pregame, update the preview card!
                updateCard(null)
            }

            for (team in state.getRegisteredTeams()) {
                updateCard(team)
            }
        }

        events.onUpdateTick {
            // Periodically send all valid cards to all players
            // to fix any missed events...

            for (player in server.playerManager.playerList) {
                if (!cardViewService.supportsCardHud(player))
                    continue

                val teams = state.getRegisteredTeams()
                val displayTeams = (teams + null).filter { cardViewService.isViewingCard(player, it) }

                val packet = CardTeams(
                    teamInfo = teams.associate { it.key to it.textColor },
                    display = displayTeams.map { it?.key },
                )

                ServerPlayNetworking.send(player, PACKET_CARD_DISPLAY, packet.toPacketBuf())

                for (team in displayTeams) {
                    val view = cardViewService.getMapView(team)
                    ServerPlayNetworking.send(player, PACKET_CARD_UPDATE, view.toPacketBuf())
                }
            }
        }

        events.onEnter(GameState.PREGAME) {
            cardService.generateCard()
        }
    }
}