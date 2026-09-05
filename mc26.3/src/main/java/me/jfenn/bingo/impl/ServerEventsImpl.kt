package me.jfenn.bingo.impl

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.impl.resource.ResourceManagerImpl
import me.jfenn.bingo.platform.BlockHitResult
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.*
import me.jfenn.bingo.platform.item.IItemStack
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.*
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.world.InteractionResult
import java.util.concurrent.CompletableFuture

class ServerEventsImpl(
    eventBus: IEventBus,
) {
    private fun getPlayerImpl(player: ServerPlayer): IPlayerHandle {
        return PlayerHandle(player, null)
    }

    private fun ActionResult<Unit>.toActionResult(): InteractionResult {
        return when (this) {
            is ActionResult.Fail -> InteractionResult.FAIL
            is ActionResult.Success -> InteractionResult.SUCCESS
            is ActionResult.Pass -> InteractionResult.PASS
        }
    }

    private fun ActionResult<IItemStack?>.toTypedActionResult(): InteractionResult {
        return takeIf { it is ActionResult.Success }
            ?.value?.stack
            ?.let {
                InteractionResult.Success(
                    InteractionResult.SwingSource.SERVER_ONLY,
                    InteractionResult.ItemContext(true, it)
                )
            }
            ?: this.map {}.toActionResult()
    }

    init {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            eventBus.emit(ServerEvent.Starting, ServerEvent(server))
        }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            eventBus.emit(ServerEvent.Started, ServerEvent(server))
        }

        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            eventBus.emit(ServerEvent.Stopped, ServerEvent(server))
        }

        ServerLifecycleEvents.AFTER_SAVE.register { server, flush, force ->
            eventBus.emit(ServerEvent.Saved, ServerEvent(server))
        }

        ServerTickEvents.START_SERVER_TICK.register {
            val event = TickEvent(it.tickCount)
            eventBus.emit(TickEvent.Start, event)
        }

        ServerTickEvents.END_SERVER_TICK.register {
            eventBus.emit(TickEvent.End, TickEvent(it.tickCount))
        }

        ServerPlayConnectionEvents.INIT.register { handler, _ ->
            val player = getPlayerImpl(handler.player)
            eventBus.emit(PlayerEvent.Init, PlayerEvent(player))
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = getPlayerImpl(handler.player)
            eventBus.emit(PlayerEvent.Join, PlayerEvent(player))
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = getPlayerImpl(handler.player)
            eventBus.emit(PlayerEvent.Disconnect, PlayerEvent(player))
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ ->
            val playerImpl = getPlayerImpl(player)
            eventBus.emit(PlayerEvent.AfterRespawn, PlayerEvent(playerImpl))
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            val server = world.server ?: return@register
            eventBus.emit(EntityLoadEvent, EntityLoadEvent(server, EntityImpl(entity), ServerWorldImpl(world)))
        }

        UseBlockCallback.EVENT.register { player, world, hand, hit ->
            if (player !is ServerPlayer || world !is ServerLevel) return@register InteractionResult.PASS
            val server = world.server ?: return@register InteractionResult.PASS
            val playerImpl = getPlayerImpl(player)
            val results = eventBus.emit(
                UseBlockEvent,
                UseBlockEvent(
                    server,
                    playerImpl,
                    world,
                    hand.toPlatformHand(),
                    BlockHitResult(
                        blockPos = BlockPosition.fromBlockPos(hit.blockPos),
                    )
                )
            )
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            if (player !is ServerPlayer || world !is ServerLevel) return@register InteractionResult.PASS
            val server = world.server ?: return@register InteractionResult.PASS
            val playerImpl = getPlayerImpl(player)
            val results = eventBus.emit(
                UseEntityEvent,
                UseEntityEvent(
                    server,
                    playerImpl,
                    ServerWorldImpl(world),
                    hand.toPlatformHand(),
                    EntityImpl(entity),
                )
            )
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: InteractionResult.PASS
        }

        UseItemCallback.EVENT.register { player, world, hand ->
            if (player !is ServerPlayer || world !is ServerLevel) return@register InteractionResult.PASS
            val server = world.server ?: return@register InteractionResult.PASS
            val playerImpl = getPlayerImpl(player)
            val results = eventBus.emit(UseItemEvent, UseItemEvent(server, playerImpl, world, hand.toPlatformHand()))
            ActionResult.collapse(results)
                ?.toTypedActionResult()
                ?: InteractionResult.PASS
        }

        AttackEntityCallback.EVENT.register { player, world, hand, entity, hit ->
            if (player !is ServerPlayer || world !is ServerLevel) return@register InteractionResult.PASS
            val server = world.server ?: return@register InteractionResult.PASS
            val playerImpl = getPlayerImpl(player)
            val results = eventBus.emit(AttackEntityEvent, AttackEntityEvent(server, playerImpl, world, hand, entity, hit))
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: InteractionResult.PASS
        }

        AttackBlockCallback.EVENT.register { player, world, hand, blockPos, direction ->
            if (player !is ServerPlayer || world !is ServerLevel) return@register InteractionResult.PASS
            val server = world.server ?: return@register InteractionResult.PASS
            val playerImpl = getPlayerImpl(player)
            val results = eventBus.emit(
                AttackBlockEvent,
                AttackBlockEvent(
                    server,
                    playerImpl,
                    world,
                    hand,
                    blockPos,
                    direction
                )
            )
            ActionResult.collapse(results)
                ?.toActionResult()
                ?: InteractionResult.PASS
        }

        ItemEvents.USE_ON.register { useOnContext ->
            val player = useOnContext.player
            val level = useOnContext.level
            if (player !is ServerPlayer || level !is ServerLevel) return@register null
            val server = level.server ?: return@register null
            val playerImpl = getPlayerImpl(player)
            eventBus.emit(
                UseItemOnEvent,
                UseItemOnEvent(
                    server,
                    playerImpl,
                    ServerWorldImpl(level, null),
                    BlockPosition.fromBlockPos(useOnContext.clickedPos),
                )
            )
            null
        }

        PlayerBlockBreakEvents.AFTER.register { level, player, blockPos, _, _ ->
            if (player !is ServerPlayer || level !is ServerLevel) return@register
            val server = level.server ?: return@register
            val playerImpl = getPlayerImpl(player)
            eventBus.emit(
                BreakBlockEvent.After,
                BreakBlockEvent(
                    server,
                    playerImpl,
                    ServerWorldImpl(level, null),
                    BlockPosition.fromBlockPos(blockPos),
                )
            )
        }

        ClientboundPlayChannelEvents.REGISTER.register { handler, _, _, _ ->
            val player = getPlayerImpl(handler.player)
            try {
                ServerPlayNetworking.getSendable(handler.player)
            } catch (_: NullPointerException) {
                // The player is not set up yet
                return@register
            }

            eventBus.emit(PlayerEvent.ChannelRegister, PlayerEvent(player))
        }

        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath(MOD_ID_BINGO, "server_reload")
        ) { store, prepareExecutor, reloadSynchronizer, applyExecutor ->
            val reloadEvent = ReloadEvent(
                resourceManager = ResourceManagerImpl(store.resourceManager()),
                prepareExecutor = prepareExecutor,
                applyExecutor = applyExecutor,
                whenPrepared = reloadSynchronizer::wait,
            )
            eventBus.emit(ReloadEvent, reloadEvent)
                .takeIf { it.isNotEmpty() }
                ?.let { CompletableFuture.allOf(*it.toTypedArray()) }
                ?: CompletableFuture.completedFuture(reloadEvent)
                    .thenCompose(reloadSynchronizer::wait)
                    .thenAcceptAsync({}, applyExecutor)
        }

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ ->
            eventBus.emit(ReloadEvent.After, ReloadEvent.After())
        }

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, _ ->
            val event = AllowChatMessageEvent(
                message = SignedMessageImpl(message),
                player = getPlayerImpl(player),
            )
            val isAllowed = eventBus.emit(AllowChatMessageEvent, event)
                .all { it }

            isAllowed
        }
    }
}