package me.jfenn.bingo.client.impl.event

import me.jfenn.bingo.client.impl.screen.ScreenHelperImpl
import me.jfenn.bingo.client.platform.event.model.*
import me.jfenn.bingo.impl.resource.ResourceManagerImpl
import me.jfenn.bingo.platform.event.IEventBus
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener

class ClientEventsImpl(
    private val eventBus: IEventBus,
) {
    init {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            eventBus.emit(ClientServerEvent.Join, ClientServerEvent())
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            eventBus.emit(ClientServerEvent.Disconnect, ClientServerEvent())
        }

        ServerboundPlayChannelEvents.REGISTER.register { _, _, _, _ ->
            eventBus.emit(ClientServerEvent.ChannelRegister, ClientServerEvent())
        }

        InvalidateRenderStateCallback.EVENT.register {
            eventBus.emit(InvalidateRenderStateEvent, InvalidateRenderStateEvent())
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            eventBus.emit(ClientTickEvent.End, ClientTickEvent())
        }

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            Identifier.fromNamespaceAndPath("yet-another-minecraft-bingo", "bingo_client"),
            object: ResourceManagerReloadListener {
                override fun onResourceManagerReload(manager: ResourceManager) {
                    eventBus.emit(ClientReloadEvent, ClientReloadEvent(ResourceManagerImpl(manager)))
                }
            },
        )

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            eventBus.emit(
                ScreenEvent.AfterInit,
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
