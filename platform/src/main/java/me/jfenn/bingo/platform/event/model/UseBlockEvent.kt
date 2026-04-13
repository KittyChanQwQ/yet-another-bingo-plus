package me.jfenn.bingo.platform.event.model

import me.jfenn.bingo.platform.BlockHitResult
import me.jfenn.bingo.platform.Hand
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.event.IReturnEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level

class UseBlockEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle,
    val world: Level,
    val hand: Hand,
    val hit: BlockHitResult,
) {
    companion object : IReturnEvent<UseBlockEvent, ActionResult<Unit>?>
}
