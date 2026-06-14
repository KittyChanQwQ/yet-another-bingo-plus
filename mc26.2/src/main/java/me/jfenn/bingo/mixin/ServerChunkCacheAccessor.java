package me.jfenn.bingo.mixin;

import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkCache.class)
public interface ServerChunkCacheAccessor {
    @Accessor("ticketStorage")
    TicketStorage getTicketStorage();

    @Invoker("runDistanceManagerUpdates")
    boolean invokeRunDistanceManagerUpdates();

    @Invoker("getChunkFutureMainThread")
    CompletableFuture<ChunkResult<ChunkAccess>> invokeGetChunkFutureMainThread(int x, int z, ChunkStatus targetStatus, boolean loadOrGenerate);
}
