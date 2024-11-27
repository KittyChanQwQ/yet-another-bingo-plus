package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.PlayerSoundCategory
import me.jfenn.bingo.platform.PlayerSoundEvent
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.player.PlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamScore
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting

class ScoreUpdateService(
    private val packetManager: ServerPacketEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val textProvider: TextProvider,
    private val playerSettingsService: PlayerSettingsService,
    private val playerManager: IPlayerManager,
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

    private fun sendScoreMessage(player: IPlayerHandle, packet: ScoreMessagePacket, fallback: Text?): Boolean {
        return when {
            packetManager.scoreMessageV2.send(player.player, packet) -> true
            packetManager.scoreMessageV1.send(player.player, packet) -> true
            else -> {
                if (fallback != null) {
                    player.sendMessage(fallback)
                }
                false
            }
        }
    }

    fun sendItemCaptured(
        scoredPlayer: PlayerProfile?,
        team: BingoTeam,
        objective: BingoObjective,
        card: BingoCard,
    ) {
        val item = objective.display.item

        val packet = ScoredItemPacket(
            player = scoredPlayer,
            isViewerOnTeam = true,
            team = team.key,
            teamColor = team.formatting,
            score = team.score.copy(items = team.score.items + 1),
            itemId = item?.identifier?.toString(),
        )

        // unless showCompletedItems=false, the exact scored item should be hidden from other teams
        val otherTeamPacket = when {
            !options.showCompletedItems -> packet.copy(isViewerOnTeam = false, score = TeamScore.ZERO, itemId = null)
            card.options.isHiddenItemsMode -> packet.copy(isViewerOnTeam = false, itemId = null)
            else -> packet.copy(isViewerOnTeam = false)
        }

        // decide if all players should get notified when a different team completes an item
        val playerList = when {
            options.showCompletedItems -> playerManager.getPlayers()
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
            if (!playerSettingsService.getPlayer(player).itemMessages)
                continue

            val isOnTeam = team.includesPlayer(player.player)
            // if playing in hidden items mode, don't reveal the item to the other teams!
            val isItemHidden = card.options.isHiddenItemsMode && !isOnTeam
            val itemText = Texts.bracketed(
                when {
                    isItemHidden -> Text.literal("*****").formatted(Formatting.OBFUSCATED)
                    else -> objective.display.name ?: Text.literal("[unknown]")
                }
            )

            val chatMessage = textProvider.string(
                StringKey.GameMessageCapturedItem,
                when {
                    packet.player != null ->
                        Text.literal("${team.symbol} ${packet.player.name}")
                            .formatted(team.formatting)
                    else -> team.getName(textProvider, playerName = true, bracketed = false)
                },
                itemText,
            ).let { formatWithTime(it) }

            val scoreMessage = ScoreMessagePacket(
                player = scoredPlayer,
                isViewerOnTeam = isOnTeam,
                item = item.takeIf { !isItemHidden },
                image = objective.display.image.takeIf { !isItemHidden },
                messageType = ScoreMessagePacket.MessageType.ITEM_SCORED,
                message = chatMessage,
            )

            sendScoreMessage(player, scoreMessage, chatMessage)
        }
    }

    fun sendItemLost(team: BingoTeam) {
        val playerList = team.players.mapNotNull { playerManager.getPlayer(it.uuid) }
        for (player in playerList) {
            when {
                packetManager.scoredItemLostV1.send(player.player, ScoredItemLostPacket()) -> {}
                else -> {
                    player.playSound(
                        PlayerSoundEvent.ENTITY_SHULKER_AMBIENT,
                        PlayerSoundCategory.RECORDS,
                        0.8f, 1.5f
                    )
                }
            }
        }
    }

    fun sendLineCaptured(team: BingoTeam) {
        if (!options.showCompletedLines) return

        val teamName = team.getName(textProvider, playerName = true)
        val message = textProvider.string(StringKey.GameMessageScoredLine, teamName, textProvider.lineCount(team.score.lines))
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        val scoreMessage = ScoreMessagePacket(
            player = null,
            isViewerOnTeam = true,
            item = null,
            messageType = ScoreMessagePacket.MessageType.LINE_SCORED,
            message = message,
        )

        playerManager.getPlayers()
            .filter { playerSettingsService.getPlayer(it).scoreMessages }
            .forEach {
                sendScoreMessage(it, scoreMessage, message)
            }
    }

    fun sendCardCompleted(team: BingoTeam, isWinner: Boolean) {
        if (isWinner) {
            val message = textProvider.string(StringKey.GameMessageCompletedCard, team.getName(textProvider, playerName = true))
                .formatted(Formatting.YELLOW)
                .let { formatWithTime(it) }

            val scoreMessage = ScoreMessagePacket(
                player = null,
                isViewerOnTeam = true,
                item = null,
                messageType = ScoreMessagePacket.MessageType.CARD_COMPLETED,
                message = message,
            )

            playerManager.getPlayers()
                .forEach {
                    sendScoreMessage(it, scoreMessage, message)
                }
        }

        val completedPacket = CardCompletedPacket(isWinner = isWinner)

        playerManager.getPlayers()
            .filter { team.includesPlayer(it) }
            .forEach {
                when {
                    packetManager.cardCompletedV1.send(it, completedPacket) -> {}
                    else -> {}
                }
            }
    }

    fun sendTeamLeading(team: BingoTeam) {
        val teamName = team.getName(textProvider, playerName = true)
        val score = team.score
        val message = when {
            options.showCompletedItems -> {
                textProvider.string(StringKey.GameMessageLeadingWithScore, teamName, textProvider.itemCount(score.items))
            }
            else -> textProvider.string(StringKey.GameMessageLeading, teamName)
        }
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        val scoreMessage = ScoreMessagePacket(
            player = null,
            isViewerOnTeam = true,
            item = null,
            messageType = ScoreMessagePacket.MessageType.LEADING_TEAM,
            message = message,
        )

        playerManager.getPlayers()
            .filter { playerSettingsService.getPlayer(it).leadingMessages }
            .forEach {
                sendScoreMessage(it, scoreMessage, message)
            }
    }

    fun sendTeamTied(team: BingoTeam) {
        val teamName = team.getName(textProvider, playerName = true)
        val score = team.score
        val message = when {
            options.showCompletedItems -> {
                textProvider.string(StringKey.GameMessageLeadingTiedWithScore, teamName, textProvider.itemCount(score.items))
            }
            else -> textProvider.string(StringKey.GameMessageLeadingTied, teamName)
        }
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        val scoreMessage = ScoreMessagePacket(
            player = null,
            isViewerOnTeam = true,
            item = null,
            messageType = ScoreMessagePacket.MessageType.LEADING_TEAM,
            message = message,
        )

        playerManager.getPlayers()
            .filter { playerSettingsService.getPlayer(it).leadingMessages }
            .forEach {
                sendScoreMessage(it, scoreMessage, message)
            }
    }

}