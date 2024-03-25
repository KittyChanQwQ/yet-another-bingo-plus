package me.jfenn.bingo.common.map

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.PACKET_CARD_DISPLAY_S2C
import me.jfenn.bingo.common.PACKET_CARD_UPDATE_S2C
import me.jfenn.bingo.common.card.BingoCardEntry
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

            for (player in server.playerManager.playerList) {
                if (cardViewService.isViewingCard(player, team) && cardViewService.supportsCardHud(player)) {
                    // yes, every send() needs its own PacketByteBuf() instance, otherwise we get Scary Errors
                    ServerPlayNetworking.send(player, PACKET_CARD_UPDATE_S2C, view.toPacketBuf())
                }
            }

            map.updatedAt = Clock.System.now()
            map.view = view
        }
    }

    init {
        events.onGameTick {
            // first populate advancement images, if missing...
            for ((identifier, entry) in state.card.advancementIndex) {
                val advancementEntry = when (entry) {
                    is BingoCardEntry.AdvancementEntry -> entry
                    is BingoCardEntry.MultiEntry -> {
                        entry.innerEntries.firstOrNull() as? BingoCardEntry.AdvancementEntry
                            ?: continue
                    }
                    else -> continue
                }

                // If the entry doesn't have a visualItem yet, set it to match the advancement
                if (advancementEntry.visualItem == null) {
                    val advancement = advancementManager.getAdvancement(server, identifier)
                        ?: continue

                    advancementEntry.visualItem = advancement.displayIcon ?: ItemStack.EMPTY
                    advancementEntry.name = advancement.name
                        ?.let { Text.empty()
                            .append(it).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GREEN)) }
                        ?.let { textSerializer.toJson(it) }
                    advancementEntry.description = advancement.description?.let { textSerializer.toJson(it) }
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

                ServerPlayNetworking.send(player, PACKET_CARD_DISPLAY_S2C, packet.toPacketBuf())

                for (team in displayTeams) {
                    val view = cardViewService.getMapView(team)
                    ServerPlayNetworking.send(player, PACKET_CARD_UPDATE_S2C, view.toPacketBuf())
                }
            }
        }

        events.onEnter(GameState.PREGAME) {
            cardService.generateCard()
        }
    }
}