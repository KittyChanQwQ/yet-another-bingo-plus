package me.jfenn.bingo.common.scoring

import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardTileImage
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamCompletedCard
import me.jfenn.bingo.common.team.TeamScore
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.PlayerSoundCategory
import me.jfenn.bingo.platform.PlayerSoundEvent
import me.jfenn.bingo.platform.player.PlayerProfile
import net.minecraft.util.Formatting

internal class ScoreUpdateService(
    private val packetManager: ServerPacketEvents,
    private val options: BingoOptions,
    private val state: BingoState,
    private val text: TextProvider,
    private val playerSettingsService: PlayerSettingsService,
    private val playerManager: IPlayerManager,
) {

    private fun formatWithTime(message: IText): IText {
        return text.empty()
            .append(
                text.literal(state.formatTimeElapsed())
                    .bracketed()
                    .formatted(Formatting.GRAY)
            )
            .append(" ")
            .append(message)
    }

    private fun sendScoreMessage(player: IPlayerHandle, packet: ScoreMessagePacket, fallback: IText?): Boolean {
        return when {
            packetManager.scoreMessageV3.send(player.player, packet) -> true
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
            teamColor = team.textColor,
            score = team.currentScore.copy(items = team.currentScore.items + 1),
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
                        val score = team.currentScore.items
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
            val itemText = when {
                isItemHidden -> text.literal("*****").formatted(Formatting.OBFUSCATED)
                else -> objective.display.name ?: text.literal("unknown")
            }.bracketed()

            val chatMessage = text.string(
                StringKey.GameMessageCapturedItem,
                when {
                    packet.player != null ->
                        text.literal(team.symbol?.plus(" ").orEmpty() + packet.player.name)
                            .formatted(team.textColor)
                    else -> team.getName(text, playerName = true, bracketed = false)
                },
                itemText,
            ).let { formatWithTime(it) }

            val scoreMessage = ScoreMessagePacket(
                player = scoredPlayer,
                isViewerOnTeam = isOnTeam,
                tile = CardTile(
                    image = CardTileImage(
                        item = item.takeIf { !isItemHidden },
                        texture = objective.display.image.takeIf { !isItemHidden },
                    ),
                    imageList = if (!isItemHidden && objective is BingoObjective.SomeOfEntry) {
                        objective.someOfObjectives
                            .mapNotNull { card.objectives[it] }
                            .take(4)
                            .map { innerObjective ->
                                CardTileImage(
                                    item = innerObjective.display.item,
                                    texture = innerObjective.display.image,
                                )
                            }
                    } else emptyList(),
                    decoration = objective.display.decoration.takeIf { !isItemHidden },
                ),
                messageType = ScoreMessagePacket.MessageType.ITEM_SCORED,
                message = chatMessage,
            )

            sendScoreMessage(player, scoreMessage, chatMessage)
        }
    }

    fun sendItemLost(team: BingoTeam, objective: BingoObjective, card: BingoCard) {
        val teamPlayers = team.players.mapNotNull { playerManager.getPlayer(it.uuid) }
        for (player in teamPlayers) {
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

        // decide if all players should get notified when a different team completes an item
        val playerList = when {
            options.showCompletedItems -> playerManager.getPlayers()
            else -> team.players.mapNotNull { playerManager.getPlayer(it.uuid) }
        }

        // Send players a message that the item was lost
        for (player in playerList) {
            // if the player has item messages turned off, skip them...
            if (!playerSettingsService.getPlayer(player).itemMessages)
                continue

            val isOnTeam = team.includesPlayer(player.player)
            // if playing in hidden items mode, don't reveal the item to the other teams!
            val isItemHidden = card.options.isHiddenItemsMode && !isOnTeam
            val itemText = when {
                isItemHidden -> text.literal("*****").formatted(Formatting.OBFUSCATED)
                else -> objective.display.name ?: text.literal("unknown")
            }.bracketed()

            val chatMessage = text.string(
                StringKey.GameMessageLostItem,
                team.getName(text, playerName = true, bracketed = false),
                itemText,
            ).let { formatWithTime(it) }

            val scoreMessage = ScoreMessagePacket(
                player = null,
                isViewerOnTeam = isOnTeam,
                tile = CardTile(
                    image = CardTileImage(
                        item = objective.display.item.takeIf { !isItemHidden },
                        texture = objective.display.image.takeIf { !isItemHidden },
                    ),
                    imageList = if (!isItemHidden && objective is BingoObjective.SomeOfEntry) {
                        objective.someOfObjectives
                            .mapNotNull { card.objectives[it] }
                            .take(4)
                            .map { innerObjective ->
                                CardTileImage(
                                    item = innerObjective.display.item,
                                    texture = innerObjective.display.image,
                                )
                            }
                    } else emptyList(),
                    decoration = objective.display.decoration.takeIf { !isItemHidden },
                ),
                messageType = ScoreMessagePacket.MessageType.ITEM_SCORED,
                message = chatMessage,
            )

            sendScoreMessage(player, scoreMessage, chatMessage)
        }
    }

    fun sendLineCaptured(team: BingoTeam) {
        if (!options.showCompletedLines) return

        val teamName = team.getName(text, playerName = true)
        val message = text.string(StringKey.GameMessageScoredLine, teamName, text.lineCount(team.score.lines))
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        val scoreMessage = ScoreMessagePacket(
            player = null,
            isViewerOnTeam = true,
            tile = null,
            messageType = ScoreMessagePacket.MessageType.LINE_SCORED,
            message = message,
        )

        playerManager.getPlayers()
            .filter { playerSettingsService.getPlayer(it).scoreMessages }
            .forEach {
                sendScoreMessage(it, scoreMessage, message)
            }
    }

    fun sendCardCompleted(team: BingoTeam, completedCard: TeamCompletedCard) {
        if (completedCard.isWinner) {
            val string = if (completedCard.isAutoWin) StringKey.GameMessageCompletedCardByStalemate else StringKey.GameMessageCompletedCard
            val message = text.string(string, team.getName(text, playerName = true))
                .formatted(Formatting.YELLOW)
                .let { formatWithTime(it) }

            val scoreMessage = ScoreMessagePacket(
                player = null,
                isViewerOnTeam = true,
                tile = null,
                messageType = ScoreMessagePacket.MessageType.CARD_COMPLETED,
                message = message,
            )

            playerManager.getPlayers()
                .forEach {
                    sendScoreMessage(it, scoreMessage, message)
                }
        }

        val completedPacket = CardCompletedPacket(isWinner = completedCard.isWinner)

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
        val teamName = team.getName(text, playerName = true)
        val score = team.score
        val message = when {
            options.showCompletedItems -> {
                text.string(StringKey.GameMessageLeadingWithScore, teamName, text.itemCount(score.items))
            }
            else -> text.string(StringKey.GameMessageLeading, teamName)
        }
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        val scoreMessage = ScoreMessagePacket(
            player = null,
            isViewerOnTeam = true,
            tile = null,
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
        val teamName = team.getName(text, playerName = true)
        val score = team.score
        val message = when {
            options.showCompletedItems -> {
                text.string(StringKey.GameMessageLeadingTiedWithScore, teamName, text.itemCount(score.items))
            }
            else -> text.string(StringKey.GameMessageLeadingTied, teamName)
        }
            .formatted(Formatting.YELLOW)
            .let { formatWithTime(it) }

        val scoreMessage = ScoreMessagePacket(
            player = null,
            isViewerOnTeam = true,
            tile = null,
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