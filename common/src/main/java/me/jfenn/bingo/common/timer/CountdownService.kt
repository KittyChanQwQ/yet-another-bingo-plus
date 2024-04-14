package me.jfenn.bingo.common.timer

import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.event.packet.ServerPacketManager
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class CountdownService(
    private val packetManager: ServerPacketManager,
    private val teamService: TeamService,
) {

    fun sendCountdownPacket(player: ServerPlayerEntity, packet: CountdownPacket) {
        if (packet.secondsRemaining in 1..3) {
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal(packet.secondsRemaining.toString())))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.empty()))
        }

        if (packet.secondsRemaining == 0) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game has started!")))
        }

        when {
            packetManager.sendPacketS2C(player, CountdownPacket.V1, packet) -> {}
            packet.secondsRemaining in 1..3 -> Sounds.playGameCountdown(player)
            packet.secondsRemaining == 0 -> Sounds.playGameStarted(player)
        }
    }

}