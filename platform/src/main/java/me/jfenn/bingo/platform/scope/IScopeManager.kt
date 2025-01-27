package me.jfenn.bingo.platform.scope

import me.jfenn.bingo.platform.utils.IEventListener
import net.minecraft.server.MinecraftServer
import org.koin.core.scope.Scope

interface IScopeManager {

    fun getScope(server: MinecraftServer?) : Scope?

    val onCreate: IEventListener<Scope>
    val onCleanup: IEventListener<Scope>

}