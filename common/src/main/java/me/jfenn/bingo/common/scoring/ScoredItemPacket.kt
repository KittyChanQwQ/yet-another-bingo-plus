package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.state.TeamScore
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

        override fun fromPacketBuf(buf: IPacketBuf): ScoredItemPacket {
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

        override fun toPacketBuf(source: ScoredItemPacket, dest: IPacketBuf) {
            dest.writeString(source.player.uuid)
            dest.writeString(source.player.name)
            dest.writeBoolean(source.isViewerOnTeam)
            dest.writeString(source.team.id)
            dest.writeString(source.teamColor.name)

            dest.writeInt(source.score.items)
            dest.writeInt(source.score.lines)

            if (source.item != null) {
                dest.writeBoolean(true)
                dest.writeString(source.item.toString())
            } else {
                dest.writeBoolean(false)
            }
        }
    }
}