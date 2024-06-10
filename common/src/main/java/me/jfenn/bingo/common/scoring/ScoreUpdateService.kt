package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.PlayerSoundCategory
import me.jfenn.bingo.api.PlayerSoundEvent
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.TeamScore
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting

class ScoreUpdateService(
    private val server: MinecraftServer,
    private val packetManager: ServerPacketEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val textProvider: TextProvider,
    private val playerSettingsService: PlayerSettingsService,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
) {

    private fun formatWithTime(message: Text): Text {
        return Text.empty()
            .append(
                Texts.bracketed(state.formatTimeElapsed())
                    .formatted(Formatting.GRAY)
            )
            .append(" ")
            .append(message)
    }

    fun sendItemCaptured(
        scoredPlayer: BingoPlayerProfile,
        team: BingoTeam,
        entry: BingoCardEntry,
        itemName: Text,
    ) {
        val packet = ScoredItemPacket(
            player = scoredPlayer,
            isViewerOnTeam = true,
            team = team.key,
            teamColor = team.formatting,
            score = team.score.copy(items = team.score.items + 1),
            itemId = entry.visualItem
                ?.identifier
                ?.toString()
        )

        // unless showOtherTeamCompletions=true, the exact scored item should be hidden from other teams
        val otherTeamPacket = when {
            options.showOtherTeamCompletions -> packet.copy(isViewerOnTeam = false)
            else -> packet.copy(isViewerOnTeam = false, score = TeamScore.ZERO, itemId = null)
        }

        // decide if all players should get notified when a different team completes an item
        val sendToEveryone = options.showOtherTeamCompletions || options.showCompletedItems
        val playerList = when {
            sendToEveryone -> playerManager.getPlayers()
            else -> team.players.mapNotNull { playerManager.getPlayer(it.uuid) }
        }

        for (player in playerList) {
            val isOnTeam = team.includesPlayer(player.player)
            val playerPacket = if (isOnTeam) packet else otherTeamPacket

            when {
                packetManager.scoredItemV1.send(player.player, playerPacket) -> {}
                else -> {
                    // play the doink sound
                    if (isOnTeam) {
                        val score = team.score.items
                        player.playSound(
                            PlayerSoundEvent.ENTITY_PLAYER_LEVELUP,
                            PlayerSoundCategory.RECORDS,
                            1f, 1f + (score / 25f)
                        )
                    } else {
                        player.playSound(
                            PlayerSoundEvent.BLOCK_NOTE_BLOCK_BASS,
                            PlayerSoundCategory.RECORDS,
                            1f, 1f
                        )
                    }
                }
            }
        }

        // Send players a message that the item was captured
        for (player in playerList) {
            // if the player has item messages turned off, skip them...
            if (!playerSettingsService.getPlayer(player.player).itemMessages)
                continue

            val isOnTeam = team.includesPlayer(player.player)
            val itemText = when {
                // if playing in hidden items mode, don't reveal the item to the other teams!
                state.options.isHiddenItemsMode && !isOnTeam -> Text.literal("*****").formatted(Formatting.OBFUSCATED)
                else -> itemName
            }.let { Texts.bracketed(it) }

            val chatMessage = textProvider.string(
                StringKey.GameMessageCapturedItem,
                Text.literal("${team.symbol} ${packet.player.name}")
                    .formatted(team.formatting),
                itemText,
            ).let { formatWithTime(it) }

            player.player.sendMessage(chatMessage, false)
        }
    }

    fun sendLineCaptured(team: BingoTeam) {
        if (!options.showCompletedLines) return

        val teamName = teamService.getTeamOrPlayerName(team)
        val message = textProvider.string(StringKey.GameMessageScoredLine, teamName, textProvider.lineCount(team.score.lines))
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        server.playerManager.playerList
            .filter { playerSettingsService.getPlayer(it).scoreMessages }
            .forEach {
                it.sendMessage(message, false)
            }
    }

    fun sendCardCompleted(team: BingoTeam) {
        val message = textProvider.string(StringKey.GameMessageCompletedCard, teamService.getTeamOrPlayerName(team))
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        server.playerManager.playerList
            .forEach { it.sendMessage(message, false) }
    }

    fun sendTeamLeading(team: BingoTeam) {
        val teamName = teamService.getTeamOrPlayerName(team)
        val score = team.score
        val message = when {
            options.showCompletedItems -> {
                textProvider.string(StringKey.GameMessageLeadingWithScore, teamName, textProvider.itemCount(score.items))
            }
            else -> textProvider.string(StringKey.GameMessageLeading, teamName)
        }
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        server.playerManager.playerList
            .filter { playerSettingsService.getPlayer(it).leadingMessages }
            .forEach { it.sendMessage(message, false) }
    }

    fun sendTeamTied(team: BingoTeam) {
        val teamName = teamService.getTeamOrPlayerName(team)
        val score = team.score
        val message = when {
            options.showCompletedItems -> {
                textProvider.string(StringKey.GameMessageLeadingTiedWithScore, teamName, textProvider.itemCount(score.items))
            }
            else -> textProvider.string(StringKey.GameMessageLeadingTied, teamName)
        }
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        server.playerManager.playerList
            .filter { playerSettingsService.getPlayer(it).leadingMessages }
            .forEach { it.sendMessage(message, false) }
    }

}