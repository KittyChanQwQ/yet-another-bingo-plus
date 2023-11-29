package me.jfenn.bingo.integrations

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import me.jfenn.bingo.Bingo
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.ScoreService
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.map.MapItemService
import me.jfenn.bingo.utils.formatTitle
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardPlayerUpdateS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.ServerScoreboard
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode

/**
 * Displays scoreboard score/timer information for all players.
 */
object ScoreboardController {

    fun getScoreboards(server: MinecraftServer): Map<BingoTeam?, ScoreboardObjective> {
        return BingoTeam.entries
            .toMutableList<BingoTeam?>()
            .apply { add(null) }
            .associateWith { team ->
                val name = "bingo_sidebar_" + team?.name
                server.scoreboard.getNullableObjective(name) ?: run {
                    server.scoreboard.addObjective(
                        name,
                        ScoreboardCriterion.DUMMY,
                        Text.literal("Minecraft BINGO"),
                        ScoreboardCriterion.RenderType.INTEGER,
                    )
                }
            }
    }

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

    fun init() {
        ConfigService.onChangePlayerSettings { player ->
            val settings = ConfigService.getPlayer(player.uuidAsString)

            if (!settings.scoreboard) {
                // send a packet remove the scoreboard sidebar if set to false
                player.networkHandler.sendPacket(createScoreboardDisplayPacket(null))
            }
        }

        Bingo.onUpdateTick {
            for ((team, objective) in scoreboards) {
                val scoreboardText = buildList {
                    add("")
                    add("Game mode: ${Formatting.YELLOW}${config.formatGameMode()}")
                    add("Features: ${Formatting.YELLOW}${config.formatFeatures()}")
                    add("Win condition: ${Formatting.YELLOW}${config.winCondition.name.formatTitle()}")
                    if (config.showRemainingTime) {
                        add("Time left: ${Formatting.YELLOW}${state.formatTimeRemaining().string}")
                    }
                    add("")
                    if (team != null) {
                        val score = state.getTeamScore(team)

                        add("Team: ${team.formatting}${team.name}")
                        add("Items scored: ${team.formatting}${score.items}")
                        add("Lines scored: ${team.formatting}${score.lines}")
                        add("")
                    }

                    val leadingTeam = ScoreService.getLeading(state)
                    if (config.showLeadingTeam) {
                        add("Leading team: ${leadingTeam?.let { it.formatting.toString() + it.name } ?: "${Formatting.WHITE}Tie"}")
                    }

                    val formatting = when {
                        // if the leading team should be hidden, don't use its formatting
                        !config.showLeadingTeam -> Formatting.WHITE
                        // otherwise, default to WHITE if the team is tied
                        else -> leadingTeam?.formatting ?: Formatting.WHITE
                    }

                    if (config.showCompletedItems) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByItems(state, allowTies = true)
                        val itemsScored = leadingOrTied?.let { state.getTeamScore(it).items } ?: 0
                        add("Items scored: $formatting$itemsScored")
                    }
                    if (config.showCompletedLines) {
                        // if the leading team is a tie, find one of the tied teams to get the item/line count
                        val leadingOrTied = leadingTeam ?: ScoreService.getLeadingByLines(state, allowTies = true)
                        val linesScored = leadingOrTied?.let { state.getTeamScore(it).lines } ?: 0
                        add("Lines scored: $formatting$linesScored")
                    }

                    if (config.showLeadingTeam || config.showCompletedItems || config.showCompletedLines)
                        add("")
                }.mapIndexed { i, str ->
                    str.ifEmpty { " ".repeat(i) }
                }

                // Remove any previous sidebar lines that have been changed
                for (score in server.scoreboard.getAllPlayerScores(objective)) {
                    if (!scoreboardText.contains(score.playerName)) {
                        server.scoreboard.resetPlayerScore(score.playerName, objective)

                        if (!server.isSingleplayer) {
                            // The server sometimes fails to send this packet, so we're adding it manually. I don't know why.
                            // - yes, this does need to be sent to all players, otherwise it may cause desync
                            server.playerManager.sendToAll(ScoreboardPlayerUpdateS2CPacket(ServerScoreboard.UpdateMode.REMOVE, objective.name, score.playerName, 0))
                        }
                    }
                }

                // Set the score of any new sidebar lines to the list index
                scoreboardText.forEachIndexed { i, text ->
                    val score = server.scoreboard.getPlayerScore(text, objective)
                    score.score = scoreboardText.size - i - 1
                }

                // Manually send the scoreboard packets to each player
                val packets = server.scoreboard.createChangePackets(objective)
                val displayPacket = createScoreboardDisplayPacket(objective)
                val hidePacket = createScoreboardDisplayPacket(null)
                server.playerManager.playerList
                    .filter { BingoTeam.fromPlayer(it) == team }
                    .filter { ConfigService.getPlayer(it.uuidAsString).scoreboard }
                    .forEach { player ->
                        // if the player is holding the map item, show the scoreboard; otherwise, hide it
                        val isHoldingMapItem = player.interactionManager.gameMode != GameMode.SURVIVAL
                                || MapItemService.isMapItem(player.mainHandStack)
                                || MapItemService.isMapItem(player.offHandStack)

                        if (server.isSingleplayer) {
                            server.scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, if (isHoldingMapItem) objective else null)
                            return@forEach
                        }

                        if (isHoldingMapItem) {
                            // change packets only need to be sent if clients are on a separate machine
                            for (packet in packets)
                                player.networkHandler.sendPacket(packet)

                            player.networkHandler.sendPacket(displayPacket)
                        } else {
                            player.networkHandler.sendPacket(hidePacket)
                        }
                    }
            }
        }
    }

}