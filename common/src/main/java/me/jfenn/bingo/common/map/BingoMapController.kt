package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
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
    private val cardService: CardService,
    private val cardViewService: CardViewService,
    private val advancementManager: IAdvancementManager,
    private val itemStackFactory: IItemStackFactory,
) : BingoComponent() {

    init {
        events.onGameTick {
            if (state.card.isDirty) {
                // first populate entry item stacks, if missing...
                for (itemEntry in state.card.getAllEntriesOf<BingoCardEntry.ItemEntry>()) {
                    // If the entry doesn't have a visualItem yet, set it to match the item
                    val stack = itemStackFactory.createStack(itemEntry.identifier)
                    itemEntry.visualItem = stack
                }

                for (advancementEntry in state.card.getAllEntriesOf<BingoCardEntry.AdvancementEntry>()) {
                    // If the entry doesn't have a visualItem yet, set it to match the advancement
                    val advancement = advancementManager.getAdvancement(server, advancementEntry.identifier)
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
                cardViewService.updateCard(null)
            }

            for (team in state.getRegisteredTeams()) {
                cardViewService.updateCard(team)
            }
        }

        events.onUpdateTick {
            // Periodically send all valid cards to all players
            // to fix any missed events...
            for (player in server.playerManager.playerList) {
                cardViewService.sendUpdatePackets(player)
            }
        }

        events.onEnter(GameState.PREGAME) {
            cardService.generateCard()
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