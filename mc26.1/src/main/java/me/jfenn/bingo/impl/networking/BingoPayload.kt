package me.jfenn.bingo.impl.networking

import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class BingoPayload<T>(
    private val id: CustomPacketPayload.Type<BingoPayload<T>>,
    val value: T,
) : CustomPacketPayload {
    override fun type() = id
}
