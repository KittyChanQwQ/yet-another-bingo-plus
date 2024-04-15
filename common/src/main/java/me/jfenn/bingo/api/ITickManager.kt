package me.jfenn.bingo.api

import net.minecraft.server.MinecraftServer

interface ITickManager {
    fun setFrozen(server: MinecraftServer, frozen: Boolean)
}

class DummyTickManager : ITickManager {
    override fun setFrozen(server: MinecraftServer, frozen: Boolean) = Unit
}
