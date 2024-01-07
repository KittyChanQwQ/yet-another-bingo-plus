package me.jfenn.bingo.impl

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import me.jfenn.bingo.api.IObjectiveHandle
import me.jfenn.bingo.api.IScoreboardManager
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardPlayerUpdateS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ServerScoreboard
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

class ScoreboardManager : IScoreboardManager {

    /**
     * Construct a ScoreboardDisplayS2CPacket using its PacketByteBuf constructor,
     * which is compatible across 1.20-1.20.2...
     */
    private fun createScoreboardDisplayPacket(objective: ScoreboardObjective?) : ScoreboardDisplayS2CPacket {
        val b = Unpooled.buffer()
        b.writeByte(1) // ScoreboardDisplaySlot.SIDEBAR.id

        val str = objective?.name ?: ""
        val strMax = ByteBufUtil.utf8MaxBytes(str)
        val strBuf = b.alloc().buffer(strMax)
        val strLen = ByteBufUtil.writeUtf8(strBuf, str)
        b.writeByte(strLen)
        b.writeBytes(strBuf)

        return ScoreboardDisplayS2CPacket(PacketByteBuf(b))
    }

    override fun createDummyObjective(server: MinecraftServer, name: String, displayName: Text): IObjectiveHandle {
        val objective = server.scoreboard.getNullableObjective(name) ?: run {
            server.scoreboard.addObjective(
                name,
                ScoreboardCriterion.DUMMY,
                displayName,
                ScoreboardCriterion.RenderType.INTEGER,
            )
        }

        return ObjectiveHandle(objective)
    }

    override fun setScoreboardText(server: MinecraftServer, handle: IObjectiveHandle, textLines: List<String>) {
        require(handle is ObjectiveHandle)

        // Remove any previous sidebar lines that have been changed
        for (score in server.scoreboard.getAllPlayerScores(handle.objective)) {
            if (!textLines.contains(score.playerName)) {
                server.scoreboard.resetPlayerScore(score.playerName, handle.objective)

                if (!server.isSingleplayer) {
                    // The server sometimes fails to send this packet, so we're adding it manually. I don't know why.
                    // - yes, this does need to be sent to all players, otherwise it may cause desync
                    for (player in server.playerManager.playerList)
                        player.networkHandler.sendPacket(ScoreboardPlayerUpdateS2CPacket(ServerScoreboard.UpdateMode.REMOVE, handle.name, score.playerName, 0))
                }
            }
        }

        textLines.forEachIndexed { i, text ->
            val score = server.scoreboard.getPlayerScore(text, handle.objective)
            score.score = textLines.size - i - 1
        }
    }

    override fun sendScoreboardDisplay(player: ServerPlayerEntity, handle: IObjectiveHandle?) {
        require(handle is ObjectiveHandle?)
        player.networkHandler.sendPacket(createScoreboardDisplayPacket(handle?.objective))
    }

    override fun sendScoreboardUpdates(player: ServerPlayerEntity, handle: IObjectiveHandle) {
        require(handle is ObjectiveHandle)
        // If the server is in singleplayer, the scoreboard packets are not handled
        // - sending these to the host (client) player will cause a bunch of error log spam
        if (player.server.isSingleplayer && player.uuid == player.server.hostProfile?.id)
            return

        val packets = player.server.scoreboard.createChangePackets(handle.objective)
        for (packet in packets) {
            if (packet is ScoreboardDisplayS2CPacket && packet.slot == 1)
                continue

            player.networkHandler.sendPacket(packet)
        }
    }

    override fun getPlayerName(player: ServerPlayerEntity): String {
        return player.entityName
    }
}

class ObjectiveHandle(
    val objective: ScoreboardObjective,
) : IObjectiveHandle {
    override val name: String
        get() = objective.name
}
