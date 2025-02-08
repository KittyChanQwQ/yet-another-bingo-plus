package me.jfenn.bingo.client.impl.event

import me.jfenn.bingo.client.impl.screen.ScreenHelperImpl
import me.jfenn.bingo.client.platform.event.IClientEvents
import me.jfenn.bingo.client.platform.event.model.ClientReloadEvent
import me.jfenn.bingo.client.platform.event.model.ScreenEvent
import me.jfenn.bingo.client.platform.event.model.ScreenType
import me.jfenn.bingo.common.utils.EventListenerFactory
import me.jfenn.bingo.platform.utils.IEventListener
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier

object ClientEventsImpl : IClientEvents {
    override val onServerJoin: IEventListener<Unit> by EventListenerFactory()
    override val onServerDisconnect: IEventListener<Unit> by EventListenerFactory()
    override val onPlayChannelRegister: IEventListener<Unit> by EventListenerFactory()
    override val onInvalidateRenderState: IEventListener<Unit> by EventListenerFactory()
    override val onClientTickEnd: IEventListener<Unit> by EventListenerFactory()
    override val onAssetsReload: IEventListener<ClientReloadEvent> by EventListenerFactory()
    override val onScreenAfterInit: IEventListener<ScreenEvent> by EventListenerFactory()

    init {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            onServerJoin(Unit)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            onServerDisconnect(Unit)
        }

        C2SPlayChannelEvents.REGISTER.register { _, _, _, _ ->
            onPlayChannelRegister(Unit)
        }

        InvalidateRenderStateCallback.EVENT.register {
            onInvalidateRenderState(Unit)
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            onClientTickEnd(Unit)
        }

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
            object : SimpleSynchronousResourceReloadListener {
                val identifier = Identifier.of("yet-another-minecraft-bingo", "bingo_client")!!

                override fun reload(manager: ResourceManager) {
                    onAssetsReload(ClientReloadEvent(manager))
                }

                override fun getFabricId(): Identifier = identifier
            }
        )

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            onScreenAfterInit(
                ScreenEvent(
                    type = when (screen) {
                        is TitleScreen -> ScreenType.TitleScreen
                        else -> ScreenType.Other
                    },
                    screen = ScreenHelperImpl(screen),
                )
            )
        }
    }
}
