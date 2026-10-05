package me.jfenn.bingo.impl

import me.jfenn.bingo.mixin.*
import me.jfenn.bingo.mixinhelper.ItemStackMixinAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.DistanceManager
import net.minecraft.server.level.ServerChunkCache
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.thread.BlockableEventLoop
import net.minecraft.world.entity.raid.Raids
import net.minecraft.world.food.FoodData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.levelgen.WorldGenSettings
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

val FoodData.accessor get() = this as FoodDataAccessor
val Raids.accessor get() = this as RaidsAccessor
val MapItemSavedData.accessor get() = this as MapItemSavedDataAccessor
val MinecraftServer.accessor get() = this as MinecraftServerAccessor
val BlockableEventLoop<*>.blockableEventLoopAccessor get() = this as BlockableEventLoopAccessor
val ServerChunkCache.accessor get() = this as ServerChunkCacheAccessor
val ChunkMap.accessor get() = this as ChunkMapAccessor
val DistanceManager.accessor get() = this as DistanceManagerAccessor
val WorldGenSettings.accessor get() = this as WorldGenSettingsAccessor
val ServerLevel.accessor get() = this as ServerLevelAccessor
val ItemStack.mixin get() = this as ItemStackMixinAccess
