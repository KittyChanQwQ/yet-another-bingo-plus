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
    val onUseBlock: IReturnEventListener<UseBlockEvent, me.jfenn.bingo.platform.event.model.ActionResult<Unit>>
    val onUseEntity: IReturnEventListener<UseEntityEvent, me.jfenn.bingo.platform.event.model.ActionResult<Unit>>
    val onUseItem: IReturnEventListener<UseItemEvent, me.jfenn.bingo.platform.event.model.ActionResult<IItemStack?>>
    val onAttackEntity: IReturnEventListener<AttackEntityEvent, me.jfenn.bingo.platform.event.model.ActionResult<Unit>>
    val onAttackBlock: IReturnEventListener<me.jfenn.bingo.platform.event.model.AttackBlockEvent, me.jfenn.bingo.platform.event.model.ActionResult<Unit>>
    val onPlayerChannelRegister: IEventListener<PlayerEvent>
    val onDataReload: IEventListener<ReloadEvent>
}