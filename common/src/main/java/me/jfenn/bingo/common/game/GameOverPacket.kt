package me.jfenn.bingo.common.game

import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamScore
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPacketBuf
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.packet.PacketConverter
import me.jfenn.bingo.platform.text.IText
import java.time.Duration
import java.time.Instant

data class GameOverPacket(
    val title: IText,
    val subtitle: IText,
    val winner: BingoTeamKey?,
    val duration: Duration,
    val isOperator: Boolean = false,
    val isReturnToLobbyAvailable: Boolean = false,
    val isResumeAvailable: Boolean = false,
    val resumeDescription: IText? = null,
    val isWinner: Boolean,
    /**
     * Whether the packet is sent because of a game state change;
     * - false indicates that the client should open the game over screen
     * - true indicates that the client should update its data without opening the screen
     */
    val isUpdate: Boolean,

    val winStreak: Long?,
    val bestWinStreak: Long? = null,
    val isBestWinStreak: Boolean = false,

    val capturedItems: Int? = null,
    val bestCapturedItems: Int? = null,
    val isBestCapturedItems: Boolean = false,

    val endedAt: Instant = Instant.MIN,
    val isBestTime: Boolean = false,
    val prevBestTime: Duration? = null,
    val seed: Long?,

    val scores: List<ScoreRanking> = emptyList(),
    val defaultTab: EndScreenTab = run {
        // If there are multiple teams & either more than one team completed the game, or the game was a draw
        if (scores.size > 1 && scores.count { it.duration != null } != 1)
            EndScreenTab.SCORES
        else EndScreenTab.CARDS
    },
) {

    enum class EndScreenTab(val title: StringKey) {
        CARDS(StringKey.GameEndTabCards),
        SCORES(StringKey.GameEndTabScores),
    }

    class ScoreRanking(
        val index: Int,
        val key: BingoTeamKey,
        val name: IText,
        val score: TeamScore,
        val duration: Duration?
    )

    object V2 : PacketConverter<GameOverPacket> {
        override val id = Identifier.of(MOD_ID_BINGO, "game_over_v2")

        override fun fromPacketBuf(buf: IPacketBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = buf.readNullable(buf::readString)?.let { BingoTeamKey(it) },
                duration = buf.readDuration(),
                isReturnToLobbyAvailable = buf.readBoolean(),
                isResumeAvailable = buf.readBoolean(),
                resumeDescription = buf.readNullable(buf::readText),
                isWinner = buf.readBoolean(),
                isUpdate = buf.readBoolean(),

                winStreak = buf.readNullable(buf::readLong),
                bestWinStreak = buf.readNullable(buf::readLong),
                isBestWinStreak = buf.readBoolean(),

                capturedItems = buf.readNullable(buf::readInt),
                bestCapturedItems = buf.readNullable(buf::readInt),
                isBestCapturedItems = buf.readBoolean(),

                endedAt = buf.readInstant(),
                isBestTime = buf.readBoolean(),
                prevBestTime = buf.readNullable(buf::readDuration),
                seed = buf.readNullable(buf::readLong),

                scores = buf.readList {
                    ScoreRanking(
                        index = buf.readInt(),
                        key = BingoTeamKey(buf.readString()),
                        name = buf.readText(),
                        score = TeamScore.V1.fromPacketBuf(buf),
                        duration = buf.readNullable(buf::readString)
                            ?.let { Duration.parse(it) }
                    )
                },
                defaultTab = try {
                    EndScreenTab.valueOf(buf.readString())
                } catch (_: IllegalArgumentException) {
                    EndScreenTab.CARDS
                },
            )
        }

        override fun toPacketBuf(source: GameOverPacket, dest: IPacketBuf) {
            dest.writeText(source.title)
            dest.writeText(source.subtitle)

            dest.writeNullable(source.winner?.id, dest::writeString)

            dest.writeDuration(source.duration)
            dest.writeBoolean(source.isReturnToLobbyAvailable)
            dest.writeBoolean(source.isResumeAvailable)
            dest.writeNullable(source.resumeDescription, dest::writeText)
            dest.writeBoolean(source.isWinner)
            dest.writeBoolean(source.isUpdate)

            dest.writeNullable(source.winStreak, dest::writeLong)
            dest.writeNullable(source.bestWinStreak, dest::writeLong)
            dest.writeBoolean(source.isBestWinStreak)

            dest.writeNullable(source.capturedItems, dest::writeInt)
            dest.writeNullable(source.bestCapturedItems, dest::writeInt)
            dest.writeBoolean(source.isBestCapturedItems)

            dest.writeInstant(source.endedAt)
            dest.writeBoolean(source.isBestTime)
            dest.writeNullable(source.prevBestTime, dest::writeDuration)

            dest.writeNullable(source.seed, dest::writeLong)

            dest.writeList(source.scores) {
                dest.writeInt(it.index)
                dest.writeString(it.key.id)
                dest.writeText(it.name)
                TeamScore.V1.toPacketBuf(it.score, dest)
                dest.writeNullable(it.duration?.toString(), dest::writeString)
            }
            dest.writeString(source.defaultTab.name)
        }
    }

    object V1 : PacketConverter<GameOverPacket> {
        override val id = Identifier.of(MOD_ID_BINGO, "game_over")

        override fun fromPacketBuf(buf: IPacketBuf): GameOverPacket {
            return GameOverPacket(
                title = buf.readText(),
                subtitle = buf.readText(),
                winner = buf.readNullable(buf::readString)?.let { BingoTeamKey(it) },
                duration = buf.readDuration(),
                isReturnToLobbyAvailable = buf.readBoolean(),
                isResumeAvailable = buf.readBoolean(),
                isWinner = buf.readBoolean(),
                isUpdate = buf.readBoolean(),

                winStreak = buf.readNullable(buf::readLong),
                bestWinStreak = buf.readNullable(buf::readLong),
                isBestWinStreak = buf.readBoolean(),

                capturedItems = buf.readNullable(buf::readInt),
                bestCapturedItems = buf.readNullable(buf::readInt),
                isBestCapturedItems = buf.readBoolean(),

                endedAt = buf.readInstant(),
                isBestTime = buf.readBoolean(),
                prevBestTime = buf.readNullable(buf::readDuration),
                seed = buf.readNullable(buf::readLong),

                scores = buf.readList {
                    ScoreRanking(
                        index = buf.readInt(),
                        key = BingoTeamKey(buf.readString()),
                        name = buf.readText(),
                        score = TeamScore.V1.fromPacketBuf(buf),
                        duration = buf.readNullable(buf::readString)
                            ?.let { Duration.parse(it) }
                    )
                },
                defaultTab = try {
                    EndScreenTab.valueOf(buf.readString())
                } catch (_: IllegalArgumentException) {
                    EndScreenTab.CARDS
                },
            )
        }

        override fun toPacketBuf(source: GameOverPacket, dest: IPacketBuf) {
            dest.writeText(source.title)
            dest.writeText(source.subtitle)

            dest.writeNullable(source.winner?.id, dest::writeString)

            dest.writeDuration(source.duration)
            dest.writeBoolean(source.isReturnToLobbyAvailable)
            dest.writeBoolean(source.isResumeAvailable)
            dest.writeBoolean(source.isWinner)
            dest.writeBoolean(source.isUpdate)

            dest.writeNullable(source.winStreak, dest::writeLong)
            dest.writeNullable(source.bestWinStreak, dest::writeLong)
            dest.writeBoolean(source.isBestWinStreak)

            dest.writeNullable(source.capturedItems, dest::writeInt)
            dest.writeNullable(source.bestCapturedItems, dest::writeInt)
            dest.writeBoolean(source.isBestCapturedItems)

            dest.writeInstant(source.endedAt)
            dest.writeBoolean(source.isBestTime)
            dest.writeNullable(source.prevBestTime, dest::writeDuration)

            dest.writeNullable(source.seed, dest::writeLong)

            dest.writeList(source.scores) {
                dest.writeInt(it.index)
                dest.writeString(it.key.id)
                dest.writeText(it.name)
                TeamScore.V1.toPacketBuf(it.score, dest)
                dest.writeNullable(it.duration?.toString(), dest::writeString)
            }
            dest.writeString(source.defaultTab.name)
        }
    }
}