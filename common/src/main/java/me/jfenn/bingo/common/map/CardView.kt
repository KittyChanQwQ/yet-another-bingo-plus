package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

data class CardView(
    val teamKey: BingoTeamKey?,
    val teamColor: Formatting?,
    val teamName: String,
    val tiles: MutableList<CardTile>,
) {

    fun tile(x: Int, y: Int): CardTile? {
        assert(x in 0 until 5) { "x=$x is not within the required 0..4 range" }
        assert(y in 0 until 5) { "y=$y is not within the required 0..4 range" }

        return tiles.getOrNull(x + y * 5)
    }

}