package me.jfenn.bingo.common

import net.minecraft.server.MinecraftServer
import org.koin.core.KoinApplication
import org.koin.core.scope.Scope

object BingoKoin {

    lateinit var koinApp: KoinApplication

    fun getScope(server: MinecraftServer?): Scope? {
        server ?: return null
        val scopeManager = koinApp.koin.get<ScopeManager>();
        return scopeManager.getScope(server)
    }

}