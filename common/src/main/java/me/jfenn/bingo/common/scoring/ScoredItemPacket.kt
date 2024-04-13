package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.state.TeamScore
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

data class ScoredItemPacket(
    val player: BingoPlayerProfile,
    val isViewerOnTeam: Boolean,
    val team: BingoTeamKey,
    val teamColor: Formatting,
    val score: TeamScore,
    val item: Identifier?,
) {
    object V1 : PacketConverter<ScoredItemPacket> {
        override val id = Identifier(MOD_ID_BINGO, "score_update")

        override fun fromPacketBuf(buf: PacketByteBuf): ScoredItemPacket {
            return ScoredItemPacket(
                player = BingoPlayerProfile(
                    uuid = buf.readString(),
                    name = buf.readString(),
                ),
                isViewerOnTeam = buf.readBoolean(),
                team = BingoTeamKey(buf.readString()),
                teamColor = Formatting.valueOf(buf.readString()),
                score = TeamScore(
                    items = buf.readInt(),
                    lines = buf.readInt(),
                ),
                item = if (buf.readBoolean()) { Identifier(buf.readString()) } else null,
            )
        }

        override fun toPacketBuf(source: ScoredItemPacket): PacketByteBuf {
            val buf = PacketByteBufs.create()
            buf.writeString(source.player.uuid)
            buf.writeString(source.player.name)
            buf.writeBoolean(source.isViewerOnTeam)
            buf.writeString(source.team.id)
            buf.writeString(source.teamColor.name)

            buf.writeInt(source.score.items)
            buf.writeInt(source.score.lines)

            if (source.item != null) {
                buf.writeBoolean(true)
                buf.writeString(source.item.toString())
            } else {
                buf.writeBoolean(false)
            }

            return buf
        }
    }
}