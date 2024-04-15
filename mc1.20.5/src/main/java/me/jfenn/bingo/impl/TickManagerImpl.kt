package me.jfenn.bingo.impl

import me.jfenn.bingo.api.ITickManager
import net.minecraft.server.MinecraftServer

class TickManagerImpl : ITickManager {
    override fun setFrozen(server: MinecraftServer, frozen: Boolean) {
        server.tickManager.isFrozen = frozen
    }
}