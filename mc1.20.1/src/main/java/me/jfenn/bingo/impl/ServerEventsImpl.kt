package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.IServerEvents
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.event.model.*
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.common.utils.IEventListener
import me.jfenn.bingo.common.utils.IReturnEventListener
import me.jfenn.bingo.common.utils.ReturnEventListener
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.*
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.TypedActionResult

object ServerEventsImpl : IServerEvents {
    override val onTickStart: IEventListener<MinecraftServer> = EventListener()
    override val onTickEnd: IEventListener<MinecraftServer> = EventListener()
    override val onPlayerJoin: IEventListener<PlayerEvent> = EventListener()
    override val onPlayerDisconnect: IEventListener<PlayerEvent> = EventListener()
    override val onAfterRespawn: IEventListener<PlayerEvent> = EventListener()
    override val onEntityLoad: IEventListener<EntityLoadEvent> = EventListener()
    override val onUseBlock: IReturnEventListener<UseBlockEvent, ActionResult<Unit>> = ReturnEventListener()
    override val onUseEntity: IReturnEventListener<UseEntityEvent, ActionResult<Unit>> = ReturnEventListener()
    override val onUseItem: IReturnEventListener<UseItemEvent, ActionResult<IItemStack?>> = ReturnEventListener()
    override val onAttackEntity: IReturnEventListener<AttackEntityEvent, ActionResult<Unit>> = ReturnEventListener()
    override val onAttackBlock: IReturnEventListener<AttackBlockEvent, ActionResult<Unit>> = ReturnEventListener()
    override val onPlayerChannelRegister: IEventListener<PlayerEvent> = EventListener()

    private fun getPlayerImpl(player: ServerPlayerEntity): IPlayerHandle? {
        val koinApp = BingoKoin.getScope(player.server) ?: return null
        val playerManager = koinApp.get<IPlayerManager>()
        return playerManager.forPlayer(player)
    }

    private fun ActionResult<Unit>.toActionResult(): net.minecraft.util.ActionResult {
        return when (this) {
            is ActionResult.Fail -> net.minecraft.util.ActionResult.FAIL
            is ActionResult.Success -> net.minecraft.util.ActionResult.SUCCESS
            is ActionResult.Pass -> net.minecraft.util.ActionResult.PASS
        }
    }

    private fun <T> ActionResult<T>.toTypedActionResult(): TypedActionResult<T> {
        return when (this) {
            is ActionResult.Fail -> TypedActionResult.fail(value)
            is ActionResult.Success -> TypedActionResult.success(value)
            is ActionResult.Pass -> TypedActionResult.pass(value)
        }
    }

    init {
        ServerTickEvents.START_SERVER_TICK.register {
            onTickStart(it)
        }

        ServerTickEvents.END_SERVER_TICK.register {
            onTickEnd(it)
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            val player = getPlayerImpl(handler.player) ?: return@register
            onPlayerJoin(PlayerEvent(server, player))
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            val player = getPlayerImpl(handler.player) ?: return@register
            onPlayerDisconnect(PlayerEvent(server, player))
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ ->
            val playerImpl = getPlayerImpl(player) ?: return@register
            onPlayerDisconnect(PlayerEvent(player.server, playerImpl))
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            onEntityLoad(EntityLoadEvent(world.server, entity, world))
        }

        UseBlockCallback.EVENT.register { player, world, hand, hit ->
            if (player !is ServerPlayerEntity) return@register net.minecraft.util.ActionResult.PASS
            val playerImpl = getPlayerImpl(player) ?: return@register net.minecraft.util.ActionResult.PASS
            val results = onUseBlock(UseBlockEvent(player.server, playerImpl, world, hand, hit))
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: net.minecraft.util.ActionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            if (player !is ServerPlayerEntity) return@register net.minecraft.util.ActionResult.PASS
            val playerImpl = getPlayerImpl(player) ?: return@register net.minecraft.util.ActionResult.PASS
            val results = onUseEntity(UseEntityEvent(player.server, playerImpl, world, hand, entity, hit))
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: net.minecraft.util.ActionResult.PASS
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            if (player !is ServerPlayerEntity) return@register TypedActionResult.pass(null)
            val playerImpl = getPlayerImpl(player) ?: return@register TypedActionResult.pass(null)
            val results = onUseItem(UseItemEvent(player.server, playerImpl, world, hand))
            ActionResult.collapse(results)
                ?.map { it?.stack }
                ?.toTypedActionResult()
                ?: TypedActionResult.pass(null)
        }

        AttackEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            if (player !is ServerPlayerEntity) return@register net.minecraft.util.ActionResult.PASS
            val playerImpl = getPlayerImpl(player) ?: return@register net.minecraft.util.ActionResult.PASS
            val results = onAttackEntity(AttackEntityEvent(player.server, playerImpl, world, hand, entity, hit))
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: net.minecraft.util.ActionResult.PASS
        }

        AttackBlockCallback.EVENT.register { player, world, hand, blockPos, direction ->
            if (player !is ServerPlayerEntity) return@register net.minecraft.util.ActionResult.PASS
            val playerImpl = getPlayerImpl(player) ?: return@register net.minecraft.util.ActionResult.PASS
            val results = onAttackBlock(AttackBlockEvent(player.server, playerImpl, world, hand, blockPos, direction))
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: net.minecraft.util.ActionResult.PASS
        }

        S2CPlayChannelEvents.REGISTER.register { handler, _, server, _ ->
            val player = getPlayerImpl(handler.player) ?: return@register
            onPlayerChannelRegister(PlayerEvent(server, player))
        }
    }
}