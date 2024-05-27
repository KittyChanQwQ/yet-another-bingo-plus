package me.jfenn.bingo.common.map

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
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
    private val packetManager: ServerPacketEvents,
    private val itemStackFactory: IItemStackFactory,
    private val mapService: IMapService,
) : BingoComponent() {

    private fun sendUpdate(player: ServerPlayerEntity, updatePacket: CardUpdatePacket) {
        when {
            packetManager.cardUpdateV2.send(player, updatePacket) -> {}
            packetManager.cardUpdateV1.send(player, updatePacket) -> {}
        }
    }

    private fun updateCard(team: BingoTeam?) {
        val view = cardViewService.getMapView(team)
        val map = team?.getMap(mapService) ?: state.getPreviewMap(mapService)

        if (cardViewService.needsUpdate(map, view)) {
            mapRenderService.update(team, map, view)

            val updatePacket = CardUpdatePacket(view)
            for (player in server.playerManager.playerList) {
                if (cardViewService.isViewingCard(player, team) && cardViewService.supportsCardHud(player)) {
                    // yes, every send() needs its own PacketByteBuf() instance, otherwise we get Scary Errors
                    sendUpdate(player, updatePacket)
                }
            }

            map.updatedAt = Clock.System.now()
            map.view = view
        }
    }

    init {
        events.onGameTick {
            if (state.card.isDirty) {
                // first populate entry item stacks, if missing...
                for (itemEntry in state.card.getAllEntriesOf<BingoCardEntry.ItemEntry>()) {
                    // If the entry doesn't have a visualItem yet, set it to match the item
                    val stack = itemStackFactory.createStack(itemEntry.identifierInstance)
                    itemEntry.visualItem = stack
                }

                for (advancementEntry in state.card.getAllEntriesOf<BingoCardEntry.AdvancementEntry>()) {
                    // If the entry doesn't have a visualItem yet, set it to match the advancement
                    val advancement = advancementManager.getAdvancement(server, advancementEntry.identifierInstance)
                        ?: continue

                    advancementEntry.visualItem = itemStackFactory.forStack(advancement.displayIcon)
                    advancementEntry.name = advancement.name
                        ?.let {
                            Text.empty()
                                .append(it)
                                .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GREEN))
                        }
                    advancementEntry.description = advancement.description
                        ?.string
                        ?.split("\n")
                        ?.map { Text.literal(it) }
                }

                state.card.isDirty = false
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

                val packet = CardDisplayPacket(
                    teamInfo = teams.associate { it.key to it.textColor },
                    display = displayTeams.map { it?.key },
                )

                packetManager.cardDisplayV1.send(player, packet)

                for (team in displayTeams) {
                    val view = cardViewService.getMapView(team)
                    sendUpdate(player, CardUpdatePacket(view))
                }
            }
        }

        events.onEnter(GameState.PREGAME) {
            cardService.generateCard()
        }
    }
}