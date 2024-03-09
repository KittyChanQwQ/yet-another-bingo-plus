package me.jfenn.bingo.common.event.model

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

class PacketEvent(
    val channel: Identifier,
    val player: ServerPlayerEntity,
    val handler: ServerPlayNetworkHandler,
    val packet: PacketByteBuf,
    val response: PacketSender,
)