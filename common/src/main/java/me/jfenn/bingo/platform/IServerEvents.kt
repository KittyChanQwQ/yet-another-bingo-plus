package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.event.model.*
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.utils.IEventListener
import me.jfenn.bingo.platform.utils.IReturnEventListener
import net.minecraft.server.MinecraftServer

interface IServerEvents {
    val onTickStart: IEventListener<MinecraftServer>
    val onTickEnd: IEventListener<MinecraftServer>
    val onPlayerJoin: IEventListener<PlayerEvent>
    val onPlayerDisconnect: IEventListener<PlayerEvent>
    val onAfterRespawn: IEventListener<PlayerEvent>
    val onEntityLoad: IEventListener<EntityLoadEvent>
    val onUseBlock: IReturnEventListener<UseBlockEvent, ActionResult<Unit>>
    val onUseEntity: IReturnEventListener<UseEntityEvent, ActionResult<Unit>>
    val onUseItem: IReturnEventListener<UseItemEvent, ActionResult<IItemStack?>>
    val onAttackEntity: IReturnEventListener<AttackEntityEvent, ActionResult<Unit>>
    val onAttackBlock: IReturnEventListener<AttackBlockEvent, ActionResult<Unit>>
    val onPlayerChannelRegister: IEventListener<PlayerEvent>
    val onDataReload: IEventListener<ReloadEvent>
    val onAllowChatMessage: IReturnEventListener<AllowChatMessageEvent, Boolean>
}