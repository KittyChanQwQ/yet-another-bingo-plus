package me.jfenn.bingo.client.platform.event

import me.jfenn.bingo.client.platform.event.model.ClientReloadEvent
import me.jfenn.bingo.platform.utils.IEventListener

interface IClientEvents {
    val onServerJoin: IEventListener<Unit>
    val onServerDisconnect: IEventListener<Unit>
    val onPlayChannelRegister: IEventListener<Unit>
    val onInvalidateRenderState: IEventListener<Unit>
    val onClientTickEnd: IEventListener<Unit>
    val onAssetsReload: IEventListener<ClientReloadEvent>
}