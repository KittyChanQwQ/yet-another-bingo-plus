package me.jfenn.bingo.impl

import me.jfenn.bingo.mixin.HungerManagerAccessor
import me.jfenn.bingo.mixin.MapStateAccessor
import me.jfenn.bingo.mixin.MinecraftServerAccessor
import me.jfenn.bingo.mixin.ThreadExecutorAccessor
import net.minecraft.entity.player.HungerManager
import net.minecraft.item.map.MapState
import net.minecraft.server.MinecraftServer
import net.minecraft.util.thread.ThreadExecutor

val HungerManager.accessor get() = this as HungerManagerAccessor
val MapState.accessor get() = this as MapStateAccessor
val MinecraftServer.accessor get() = this as MinecraftServerAccessor
val ThreadExecutor<*>.threadExecutorAccessor get() = this as ThreadExecutorAccessor
