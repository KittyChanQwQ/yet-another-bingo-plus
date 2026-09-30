package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.chat.CoordsCommand
import me.jfenn.bingo.common.map.BingoMap
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.BlockPositionType
import me.jfenn.bingo.common.utils.Vector3dAsArray
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IServerWorld
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.player.PlayerProfile
import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.HoverAction
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.text.ITextSerialized
import me.jfenn.bingo.platform.utils.UuidAsString
import net.minecraft.ChatFormatting
import net.minecraft.server.level.ServerPlayer
import org.joml.Vector3d
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

@Serializable
@JvmInline
value class BingoTeamKey(
    val id: String,
) {
    val label get() = id.removePrefix("bingo_")
}

@Serializable
internal data class BingoTeam(
    val id: String,
    var name: ITextSerialized,
    val shouldFormatName: Boolean = false,
    val symbol: String? = null,
    var textColor: Formatting = Formatting.RESET,
    val isTemporary: Boolean = false,
    var state: TeamState = TeamState.PendingCard(null),
    var currentScore: TeamScore = TeamScore.ZERO,
    /**
     * Used when winCondition=ReplaceItems to track scored lines/items that no longer exist on the card
     */
    var persistentScore: TeamScore = TeamScore.ZERO,
    val completedCards: MutableList<TeamCompletedCard> = mutableListOf(),
    var spawnpoint: BlockPositionType? = null,
    var chestSpawnpoint: BlockPositionType? = null,
    val chatCoordinates: MutableList<CoordsCommand.CoordinatesEntry> = mutableListOf(),
    var map: BingoMap? = null,
    private val playerMap: MutableMap<UuidAsString, Player> = mutableMapOf(),
) {

    @Serializable
    data class Player(
        val uuid: UuidAsString,
        val name: String,
        val randomOffset: Vector3dAsArray = Vector3d(Random.nextDouble(), 0.0, Random.nextDouble()),
    ) {
        constructor(profile: PlayerProfile): this(profile.uuid, profile.name)
        val profile get() = PlayerProfile(uuid, name)
    }

    val players: Collection<Player> get() = playerMap.values
    fun player(uuid: UUID) = playerMap[uuid]
    fun addPlayer(profile: PlayerProfile) = playerMap.putIfAbsent(profile.uuid, Player(profile)) == null
    fun removePlayer(uuid: UUID) = playerMap.remove(uuid) != null

    @Transient
    var scoreUpdatedAt: Instant = Instant.MIN

    val cardId get() = (state as? TeamState.Playing)?.cardId

    /**
     * Creates a copy of the team that does not share any mutable
     * references with the original
     */
    fun newInstance(): BingoTeam {
        return copy(
            completedCards = mutableListOf(),
            chatCoordinates = mutableListOf(),
            playerMap = mutableMapOf(),
        )
    }

    @Transient
    var spawnpointFuture: CompletableFuture<BlockPosition>? = null

    @Transient
    var spawnpointTicket: IServerWorld.IChunkTicketHandle? = null

    val key get() = BingoTeamKey(id)

    val mapColor get() = when (textColor) {
        // gray is indistinguishable from the preview card map item
        Formatting.GRAY -> Formatting.DARK_GRAY
        else -> textColor
    }

    val singlePlayer get() = playerMap.values.takeIf { it.size == 1 }?.first()

    val score get() = currentScore + persistentScore

    fun isFinished(): Boolean = this.state is TeamState.Finished

    fun isPlaying() = !isFinished()

    fun includesPlayer(player: IPlayerHandle): Boolean {
        return playerMap.containsKey(player.uuid)
    }

    fun includesPlayer(player: ServerPlayer): Boolean {
        return playerMap.containsKey(player.uuid)
    }

    fun includesPlayer(profile: PlayerProfile) = playerMap.containsKey(profile.uuid)

    fun getSimpleName() = name.copy()

    fun getSymbolText(textProvider: TextProvider): IText? {
        return playerMap.values.takeIf { it.size == 1 }
            ?.firstOrNull()
            ?.let { textProvider.player(it.uuid)?.formatted(ChatFormatting.WHITE)?.append(" ") }
            ?: symbol?.let { textProvider.literal("$it ") }
    }

    fun getName(
        textProvider: TextProvider,
        playerName: Boolean = false,
        symbol: Boolean = playerName && playerMap.size == 1,
        bracketed: Boolean = true,
        teamNameKey: StringKey? = StringKey.TeamName,
    ): IText {
        val teamName = (if (playerName) singlePlayer?.let { textProvider.literal(it.name) } else null)
            ?: (if (shouldFormatName && teamNameKey != null) textProvider.string(teamNameKey, getSimpleName()) else null)
            ?: getSimpleName()

        val teamSymbol = getSymbolText(textProvider)

        return teamName
            .let { if (symbol && teamSymbol != null) textProvider.empty().append(teamSymbol).append(it) else it }
            .formatted(this.textColor)
            .let { if (bracketed) it.bracketed() else it }
            .also { t ->
                t.setHoverEvent(
                    HoverAction.ShowText(
                        textProvider.joinText(
                            list = playerMap.values.map {
                                (textProvider.player(it.uuid)?.append(" ") ?: textProvider.empty())
                                    .append(it.name)
                            },
                            separator = textProvider.literal("\n"),
                        )
                    )
                )
            }
    }

    fun countCards(): Int = completedCards.count { it.isWinner }

    companion object {
        fun fromPreset(key: BingoTeamKey, preset: BingoTeamPreset): BingoTeam {
            return BingoTeam(
                id = key.id,
                name = preset.name,
                shouldFormatName = preset.shouldFormatName,
                symbol = preset.symbol,
                textColor = preset.color,
            )
        }
    }

}