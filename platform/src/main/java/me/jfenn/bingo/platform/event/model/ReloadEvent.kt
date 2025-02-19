package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.event.IEvent
import net.minecraft.resource.ResourceManager

data class ReloadEvent(
    val resourceManager: ResourceManager,
) {
    companion object : IEvent<ReloadEvent>
}
