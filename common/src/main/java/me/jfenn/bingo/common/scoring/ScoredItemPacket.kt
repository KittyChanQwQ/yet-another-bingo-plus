package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamScore
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter

data class ScoredItemPacket(
    val isViewerOnTeam: Boolean,
    val team: BingoTeamKey,
    val score: TeamScore,
) {
    object V1 : PacketConverter<ScoredItemPacket> {
        override val id = Identifier.of(MOD_ID_BINGO, "score_update")

        override fun fromPacketBuf(buf: IPacketBuf): ScoredItemPacket {
            return ScoredItemPacket(
                isViewerOnTeam = buf.readBoolean(),
                team = BingoTeamKey(buf.readString()),
                score = TeamScore.V1.fromPacketBuf(buf),
            )
        }

        override fun toPacketBuf(source: ScoredItemPacket, dest: IPacketBuf) {
            dest.writeBoolean(source.isViewerOnTeam)
            dest.writeString(source.team.id)
            TeamScore.V1.toPacketBuf(source.score, dest)
        }
    }
}