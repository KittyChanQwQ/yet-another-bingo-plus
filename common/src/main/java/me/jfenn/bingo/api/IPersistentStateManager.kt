package me.jfenn.bingo.api

import net.minecraft.server.world.ServerWorld
import net.minecraft.world.PersistentState
import kotlin.reflect.KClass

interface IPersistentStateManager {

    fun <T: PersistentState> getFromWorld(world: ServerWorld, clazz: KClass<T>, id: String): T

}