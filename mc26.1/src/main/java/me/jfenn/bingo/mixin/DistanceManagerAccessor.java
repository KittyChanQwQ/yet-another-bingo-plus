package me.jfenn.bingo.mixin;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DistanceManager.class)
public interface DistanceManagerAccessor {
    @Invoker("getChunk")
    ChunkHolder invokeGetChunk(long pos);
}
