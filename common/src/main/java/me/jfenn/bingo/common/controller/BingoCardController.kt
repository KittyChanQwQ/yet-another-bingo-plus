package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.MapRenderService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger

/**
 * Handles re-rolling or generating new BINGO card layouts.
 */
class BingoCardController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val mapRenderService: MapRenderService,
    private val cardService: CardService,
    private val advancementManager: IAdvancementManager,
    private val textSerializer: ITextSerializer,
) : BingoComponent() {
    init {
        events.onGameTick {
            // first populate advancement images, if missing...
            for (entry in state.card.advancementEntries) {
                val advancement = advancementManager.getAdvancement(server, entry.identifierInstance)
                    ?: continue

                // If the entry doesn't have a visualItem yet, set it to match the advancement
                if (entry.visualItem == null) {
                    entry.visualItem = advancement.displayIcon ?: ItemStack.EMPTY
                    entry.name = advancement.name?.let { textSerializer.toJson(it) }
                    entry.description = advancement.description?.let { textSerializer.toJson(it) }
                }
            }

            // update all card map images
            if (state.state == GameState.PREGAME) {
                // if in pregame, update the preview card!
                mapRenderService.update(null)
            } else {
                for (team in state.getRegisteredTeams()) {
                    mapRenderService.update(team)
                }
            }
        }

        events.onEnter(GameState.PREGAME) {
            cardService.generateCard()
        }

        events.onEnter(GameState.POSTGAME) {
            // Mark all items as "seen" when game ends
            // (if hidden items is true, players should see all items after the game)
            for (team in state.teams.values) {
                for (item in state.card.itemIndex.values) {
                    item.markSeen(team)
                }
            }
        }
    }
}