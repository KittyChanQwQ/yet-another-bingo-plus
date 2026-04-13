package me.jfenn.bingo.client.platform.event.model

import me.jfenn.bingo.platform.event.IEvent
import me.jfenn.bingo.platform.resource.IResourceManager

class ClientReloadEvent(
    val resourceManager: IResourceManager,
) {
    companion object : IEvent<ClientReloadEvent>
}