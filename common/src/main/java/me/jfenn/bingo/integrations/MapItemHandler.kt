package me.jfenn.bingo.integrations

import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.map.MapItemService
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.BoatItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.util.hit.HitResult
import java.util.*

object MapItemHandler {

    // set of players for which the bingo map item was used in the current tick
    private val usedBingoMap = mutableSetOf<UUID>()
    // set of players for which *any* item was used in the current tick
    private val usedAnyItem = mutableSetOf<UUID>()

    private val itemCooldown = mutableMapOf<UUID, Int>()

    private fun isItemUsable(itemStack: ItemStack, player: PlayerEntity): Boolean {
        // vaguely determine if the held item will be used or not
        // (e.g. suspicious stew, potions, etc. - vs. static items such as iron ingots)
        val isUsable = when {
            itemStack.isEmpty -> false
            itemStack.useAction == UseAction.EAT -> player.canConsume(false)
            itemStack.useAction == UseAction.DRINK -> true
            itemStack.useAction == UseAction.BLOCK -> true // blocking with a shield
            itemStack.item is BoatItem -> true // trying to place a boat
            itemStack.item is BlockItem -> {
                // if holding a block, check if it can be placed on a surface within reach
                val hit = player.raycast(5.0, 0f, false)
                hit.type == HitResult.Type.BLOCK
            }
            itemStack.item == Items.FIREWORK_ROCKET -> player.isFallFlying || player.isTouchingWater
            itemStack.item == Items.SUSPICIOUS_STEW -> true
            itemStack.isUsedOnRelease -> true
            itemStack.useAction == UseAction.NONE -> false
            else -> true
        }

        return isUsable
    }

    fun init() {
        UseItemCallback.EVENT.register { player, world, hand ->
            if (player !is ServerPlayerEntity) return@register TypedActionResult.pass(null)
            val ctx = player.server?.let { Bingo.getContext(it) } ?: return@register TypedActionResult.pass(null)
            val itemStack = player.getStackInHand(hand)
            val team = BingoTeam.fromPlayer(player) ?: return@register TypedActionResult.pass(itemStack)

            usedAnyItem.add(player.uuid)

            // if the item is the player's bingo map, open the item details chest
            if (itemStack.item == Items.FILLED_MAP && itemStack.nbt?.getInt("map") == team.getMapId(ctx)) {
                val otherItemStack = player.getStackInHand(when (hand) {
                    Hand.MAIN_HAND -> Hand.OFF_HAND
                    else -> Hand.MAIN_HAND
                })

                if (hand == Hand.OFF_HAND && !otherItemStack.isEmpty)
                    return@register TypedActionResult.pass(itemStack)

                if (hand == Hand.MAIN_HAND && isItemUsable(otherItemStack, player))
                    return@register TypedActionResult.pass(itemStack)

                usedBingoMap.add(player.uuid)
                return@register TypedActionResult.success(itemStack, false)
            }

            // if the item *can* be used, and *isn't* a bingo map, it should prevent the bingo map from being used
            if (usedBingoMap.contains(player.uuid) && isItemUsable(itemStack, player)) {
                // println("Removing; item used ($hand, with ${itemStack.translationKey})")
                usedBingoMap.remove(player.uuid)
            }

            TypedActionResult.pass(itemStack)
        }

        UseBlockCallback.EVENT.register { player, world, hand, hit ->
            usedAnyItem.add(player.uuid)

            val itemStack = player.getStackInHand(hand)

            if (usedBingoMap.contains(player.uuid) && !MapItemService.isMapItem(itemStack) && !itemStack.isEmpty) {
                // println("Removing; block used ($hand, with ${itemStack.translationKey})")
                usedBingoMap.remove(player.uuid)
            }

            ActionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            usedAnyItem.add(player.uuid)

            val itemStack = player.getStackInHand(hand)

            if (usedBingoMap.contains(player.uuid) && !MapItemService.isMapItem(itemStack) && !itemStack.isEmpty) {
                // println("Removing; entity used")
                usedBingoMap.remove(player.uuid)
            }

            ActionResult.PASS
        }

        ServerTickEvents.START_SERVER_TICK.register { server ->
            val ctx = Bingo.getContext(server) ?: return@register

            for (player in server.playerManager.playerList) {
                // ensure the player waits at least 10 ticks before using the map
                // - if the use button is held, this will continually update the cooldown, and never exit this state until released
                val isInCooldown = server.ticks < (itemCooldown[player.uuid] ?: 0) + 10

                if (usedBingoMap.contains(player.uuid) && !isInCooldown && !player.isSneaking) {
                    CardService.showInventory(player, ctx.state.card)
                }

                if (usedAnyItem.contains(player.uuid) || player.itemUseTimeLeft > 0)
                    itemCooldown[player.uuid] = server.ticks
            }

            usedBingoMap.clear()
            usedAnyItem.clear()
        }
    }

}