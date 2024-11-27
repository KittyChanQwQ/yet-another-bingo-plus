package me.jfenn.bingo.common.timer

import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class CountdownService(
    private val packetManager: ServerPacketEvents,
    private val teamService: TeamService,
    private val playerManager: IPlayerManager,
    private val textProvider: TextProvider,
) {

    fun sendCountdownPacket(player: ServerPlayerEntity, packet: CountdownPacket) {
        if (packet.secondsRemaining in 1..3) {
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal(packet.secondsRemaining.toString())))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.empty()))
        }

        if (packet.secondsRemaining == 0) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(textProvider.string(StringKey.GameAnnounceBingo).formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(textProvider.string(StringKey.GameAnnounceStarted)))
        }

        when {
            packetManager.countdownV1.send(player, packet) -> {}
            packet.secondsRemaining in 1..3 -> Sounds.playGameCountdown(playerManager.forPlayer(player))
            packet.secondsRemaining == 0 -> Sounds.playGameStarted(playerManager.forPlayer(player))
        }
    }

}