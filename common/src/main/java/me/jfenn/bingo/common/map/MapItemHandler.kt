package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.CardScreenHandler
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Hand
import java.util.*

internal class MapItemHandler(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val mapItemService: MapItemService,
    private val itemStackFactory: IItemStackFactory,
    private val teamService: TeamService,
    private val state: BingoState,
) : BingoComponent() {

    // set of players for which the bingo map item was used in the current tick
    private val usedBingoMap = mutableSetOf<UUID>()
    // set of players for which *any* item was used in the current tick
    private val usedAnyItem = mutableSetOf<UUID>()

    private val itemCooldown = mutableMapOf<UUID, Int>()

    private fun showInventory(player: ServerPlayerEntity) {
        val team = teamService.getPlayerTeam(player)
        val view = when {
            team == null -> state.previewMap?.view
            else -> team.map?.view
        } ?: return

        player.openHandledScreen(
            SimpleNamedScreenHandlerFactory(
            { syncId, inv, _ ->
                CardScreenHandler(
                    syncId = syncId,
                    view = view,
                    itemStackFactory = itemStackFactory,
                    playerInventory = inv,
                )
            },
            Text.literal("BINGO Card")
        )
        )
    }

    init {
        events.onUseItem { event ->
            val player = event.player.player
            val hand = event.hand
            val itemStack = player.getStackInHand(hand)
                ?.let { itemStackFactory.forStack(it) }
                ?: return@onUseItem null

            usedAnyItem.add(player.uuid)

            // if the item is the player's bingo map, open the item details chest
            if (mapItemService.isMapItem(itemStack)) {
                val otherItemStack = player.getStackInHand(when (hand) {
                    Hand.MAIN_HAND -> Hand.OFF_HAND
                    else -> Hand.MAIN_HAND
                }).let { itemStackFactory.forStack(it) }

                if (hand == Hand.OFF_HAND && !otherItemStack.isEmpty)
                    return@onUseItem me.jfenn.bingo.platform.event.model.ActionResult.Pass(itemStack)

                if (hand == Hand.MAIN_HAND && event.player.canUseItem(otherItemStack))
                    return@onUseItem me.jfenn.bingo.platform.event.model.ActionResult.Pass(itemStack)

                usedBingoMap.add(player.uuid)
                return@onUseItem me.jfenn.bingo.platform.event.model.ActionResult.Success(itemStack)
            }

            // if the item *can* be used, and *isn't* a bingo map, it should prevent the bingo map from being used
            if (usedBingoMap.contains(player.uuid) && event.player.canUseItem(itemStack)) {
                // println("Removing; item used ($hand, with ${itemStack.translationKey})")
                usedBingoMap.remove(player.uuid)
            }

            me.jfenn.bingo.platform.event.model.ActionResult.Pass(itemStack)
        }

        events.onUseBlock { event ->
            val player = event.player
            usedAnyItem.add(player.uuid)

            val itemStack = player.player.getStackInHand(event.hand)
                ?.let { itemStackFactory.forStack(it) }
                ?: return@onUseBlock null

            if (
                usedBingoMap.contains(player.uuid)
                && !mapItemService.isMapItem(itemStack)
                && !itemStack.isEmpty
            ) {
                // println("Removing; block used ($hand, with ${itemStack.translationKey})")
                usedBingoMap.remove(player.uuid)
            }

            me.jfenn.bingo.platform.event.model.ActionResult.Pass(Unit)
        }

        events.onUseEntity { event ->
            val player = event.player
            usedAnyItem.add(player.uuid)

            val itemStack = player.player.getStackInHand(event.hand)
                ?.let { itemStackFactory.forStack(it) }
                ?: return@onUseEntity null

            if (
                usedBingoMap.contains(player.uuid)
                && !mapItemService.isMapItem(itemStack)
                && !itemStack.isEmpty
            ) {
                // println("Removing; entity used")
                usedBingoMap.remove(player.uuid)
            }

            me.jfenn.bingo.platform.event.model.ActionResult.Pass(Unit)
        }

        events.onGameTick {
            for (player in server.playerManager.playerList) {
                // ensure the player waits at least 10 ticks before using the map
                // - if the use button is held, this will continually update the cooldown, and never exit this state until released
                val isInCooldown = server.ticks < (itemCooldown[player.uuid] ?: 0) + 10

                if (usedBingoMap.contains(player.uuid) && !isInCooldown && !player.isSneaking) {
                    showInventory(player)
                }

                if (usedAnyItem.contains(player.uuid) || player.itemUseTimeLeft > 0)
                    itemCooldown[player.uuid] = server.ticks
            }

            usedBingoMap.clear()
            usedAnyItem.clear()
        }
    }

}