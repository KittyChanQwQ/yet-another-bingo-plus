package me.jfenn.bingo.common.options

import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.commands.BingoOptionsCommands
import me.jfenn.bingo.common.controller.GameRuleController
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.spawn.ElytraService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal val IExecutionContext.optionsContext
    get() = OptionsService.Context(
        player = player,
        receiveFeedback = ::sendFeedback,
        receiveError = ::error,
    )

internal class OptionsService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val cardService: CardService,
    private val cardViewService: CardViewService,
    private val elytraService: ElytraService,
    private val objectiveFilterService: ObjectiveFilterService,
    private val gameRuleController: GameRuleController,
    private val teamService: TeamService,
    private val playerManager: IPlayerManager,
    private val text: TextProvider,
) {
    data class Context(
        val player: IPlayerHandle?,
        val receiveFeedback: ((Text) -> Unit)? = null,
        val receiveError: ((Text) -> Nothing)? = null,
    )

    fun broadcastHotbarMessage(player: IPlayerHandle, message: Text) {
        playerManager.getPlayers()
            .filterNot { it.uuid == player.uuid }
            .forEach {
                it.sendHotbarMessage(
                    Text.translatable("chat.type.admin", player.playerName, message)
                        .formatted(Formatting.GRAY, Formatting.ITALIC)
                )
            }
    }

    fun Context.sendFeedback(message: Text) {
        receiveFeedback?.invoke(message)
            ?: player?.let { player ->
                broadcastHotbarMessage(player, message)
            }
    }

    fun Context.error(message: Text) {
        receiveError?.invoke(message)
        throw IllegalArgumentException(message.string)
    }

    fun setGoal(
        ctx: Context,
        card: BingoCard = cardViewService.getPlayerCard(ctx.player),
        goal: BingoGoal,
    ) {
        // If ReplaceGoals is enabled, we don't need to limit max items/lines...
        val supportsFullCard = options.winCondition !is BingoWinCondition.ReplaceGoals
        val coercedGoal = if (supportsFullCard) {
            when (goal) {
                is BingoGoal.Lines -> BingoGoal.Lines(goal.lines.coerceAtMost(BingoGoal.MAX_LINES))
                is BingoGoal.Items -> BingoGoal.Items(goal.items.coerceAtMost(BingoGoal.MAX_ITEMS))
            }
        } else goal

        card.options.goal = coercedGoal
        // Send a message that the goal was changed
        val goalName = coercedGoal.format(
            textProvider = text,
            supportsFullCard = supportsFullCard,
        )
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsGoal, goalName.formatted(Formatting.WHITE))
        )
    }

    fun setCardDifficulty(
        ctx: Context,
        card: BingoCard = cardViewService.getPlayerCard(ctx.player),
        itemDistInput: List<Int>,
        allowInvalid: Boolean = false,
    ) {
        val itemDist = List(5) { itemDistInput.getOrNull(it) ?: 0 }

        if (itemDist.sum() > 25 && !allowInvalid) {
            ctx.error(text.string(StringKey.CommandStartInvalidDistribution))
        }

        card.options.itemDistribution = itemDist
        // when changed, update the bingo card
        if (card.options.isValid()) {
            cardService.generateCard(card)
        }
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsCardDifficulty, card.options.formatItemDist())
        )
    }

    fun setCardFilter(
        ctx: Context,
        card: BingoCard = cardViewService.getPlayerCard(ctx.player),
        filter: ObjectiveFilterList,
        includePresetDetails: Boolean = false
    ) {
        card.options.itemFilter = filter
        // when changed, update the bingo card
        if (card.options.isValid()) {
            cardService.generateCard(card)
        }
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsFilter, objectiveFilterService.formatFilter(filter, includePresetDetails).formatted(Formatting.WHITE))
        )
    }

    fun toggleCardMode(
        ctx: Context,
        card: BingoCard = cardViewService.getPlayerCard(ctx.player),
        prop: KMutableProperty1<BingoCardOptions, Boolean>,
        value: Boolean? = null,
    ) {
        val newValue = value ?: (!prop.get(card.options))
        prop.set(card.options, newValue)

        // inventory mode & consume items mode are not compatible
        if (prop == BingoCardOptions::isConsumeItemsMode) {
            card.options.isInventoryMode = false
        }
        if (prop == BingoCardOptions::isInventoryMode) {
            card.options.isConsumeItemsMode = false
        }

        ctx.sendFeedback(
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsGameMode,
                options.formatGameMode(card)
                    .map { text.string(it) }
                    .joinText()
                    .formatted(Formatting.WHITE),
            )
        )
    }

    fun setWinCondition(
        ctx: Context,
        winCondition: BingoWinCondition,
    ) {
        options.winCondition = winCondition

        // Send a message that the goal was changed
        val describe = when (winCondition) {
            is BingoWinCondition.Cards -> text.cardCount(winCondition.cards)
            is BingoWinCondition.Infinite -> text.string(StringKey.OptionsWinConditionInfinite)
            is BingoWinCondition.ReplaceGoals -> text.string(StringKey.OptionsWinConditionReplaceGoals)
        }
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsWinCondition, describe.formatted(Formatting.WHITE))
        )
    }

    fun toggleInfiniteMode(
        ctx: Context,
    ) {
        val newWinCondition = when (options.winCondition) {
            is BingoWinCondition.ReplaceGoals -> BingoWinCondition.Infinite
            is BingoWinCondition.Cards -> BingoWinCondition.Infinite
            is BingoWinCondition.Infinite -> BingoWinCondition.Cards(1)
        }

        setWinCondition(ctx, newWinCondition)
    }

    fun toggleReplaceGoals(
        ctx: Context,
    ) {
        val newWinCondition = when (options.winCondition) {
            is BingoWinCondition.Cards -> BingoWinCondition.ReplaceGoals
            is BingoWinCondition.Infinite -> BingoWinCondition.ReplaceGoals
            is BingoWinCondition.ReplaceGoals -> BingoWinCondition.Cards(1)
        }

        setWinCondition(ctx, newWinCondition)
    }

    fun setEndWhen(
        ctx: Context,
        endWhen: EndWhen,
    ) {
        options.endGameWhen = endWhen
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsWinBehaviorEndWhen, endWhen.string(text).formatted(Formatting.WHITE))
        )
    }

    fun toggleKeepInventory(
        ctx: Context,
        isKeepInventory: Boolean = !options.isKeepInventory,
    ) {
        options.isKeepInventory = isKeepInventory
        gameRuleController.writeToServer()
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsKeepInventory, text.boolean(isKeepInventory))
        )
    }

    fun togglePreviewCard(
        ctx: Context,
        showPreviewCard: Boolean = !options.showPreviewCard,
    ) {
        options.showPreviewCard = showPreviewCard
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsPreviewCard, text.boolean(showPreviewCard))
        )
    }

    fun toggleElytra(
        ctx: Context,
        isElytra: Boolean = !options.isElytra,
    ) {
        options.isElytra = isElytra

        // if Elytra mode is turned off during a game, remove all players' elytra
        if (!isElytra && state.isLobbyMode) {
            for (player in playerManager.getPlayers()) {
                elytraService.takeElytra(player)
            }
        }

        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsElytra, text.boolean(isElytra))
        )
    }

    fun toggleNightVision(
        ctx: Context,
        isNightVision: Boolean = !options.isNightVision,
    ) {
        options.isNightVision = isNightVision

        // if night vision is changed, update all players
        if (state.state == GameState.PLAYING) {
            playerManager.getPlayers().forEach { player ->
                state.playersJoinedIds.add(player.uuid)
            }
        }

        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsNightVis, text.boolean(isNightVision))
        )
    }

    fun togglePvp(
        ctx: Context,
        isPvpEnabled: Boolean = !options.isPvpEnabled,
    ) {
        options.isPvpEnabled = isPvpEnabled
        gameRuleController.writeToServer()
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsAllowPvp, text.boolean(isPvpEnabled))
        )
    }

    fun toggleUnlockRecipes(
        ctx: Context,
        isUnlockRecipes: Boolean = !options.isUnlockRecipes,
    ) {
        options.isUnlockRecipes = isUnlockRecipes
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsUnlockRecipes, text.boolean(isUnlockRecipes))
        )
    }

    fun setTimeLimit(
        ctx: Context,
        minutes: Int?
    ) {
        options.timeLimit = minutes
            ?.takeIf { it > 0 }
            ?.minutes
            ?.plus(state.ingameDuration() ?: Duration.ZERO)

        ctx.sendFeedback(
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsTimeLimit,
                when {
                    options.timeLimit != null -> state.formatTimeRemaining(text)
                    else -> text.string(StringKey.OptionsTimeLimitOff)
                }
            )
        )
    }

    fun setSpawnDistance(ctx: Context, distance: Int) {
        options.spawnDistance = distance
        ctx.sendFeedback(
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsSpawnDistance,
                text.string(StringKey.OptionsSpawnDistanceChunks, distance)
            )
        )
    }

    fun setSpawnDimension(ctx: Context, dimension: String) {
        options.spawnDimension = dimension
        val label = Text.literal(dimension.substringAfterLast(':').formatTitle())
        ctx.sendFeedback(text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsSpawnDimension, label))
    }

    fun getOptionsSummary(player: IPlayerHandle?): Sequence<MutableText> = sequence {
        val team = player?.let { teamService.getPlayerTeam(it) }
        val card = cardViewService.getPlayerCard(player)

        text.string(
            StringKey.ScoreboardMode,
            options.formatGameMode(card)
                .map { text.string(it) }
                .joinText()
                .formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo mode"))
            }
            .let { yield(it) }

        text.string(
            StringKey.ScoreboardFeatures,
            options.formatFeatures()
                .map { text.string(it) }
                .joinText()
                .formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .let { yield(it) }

        text.string(StringKey.OptionsGoal)
            .append(": ")
            .append(state.formatCardGoals(team, text).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo goal"))
            }
            .let { yield(it) }

        if (options.endGameWhen !is EndWhen.FirstWin) {
            text.string(
                StringKey.OptionsEndWhen,
                options.endGameWhen.string(text).formatted(Formatting.YELLOW)
            )
                .formatted(Formatting.GRAY)
                .also {
                    it.style = it.style.withClickEvent(
                        ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            BingoOptionsCommands.END_WHEN
                        )
                    )
                }
                .let { yield(it) }
        }

        options.timeLimit?.let { timeLimit ->
            text.string(StringKey.OptionsTimeLimit)
                .append(": ")
                .append(
                    timeLimit.inWholeMinutes.let { minutes ->
                        String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
                            .let { Text.literal(it).formatted(Formatting.YELLOW) }
                    }
                )
                .formatted(Formatting.GRAY)
                .also {
                    it.style = it.style.withClickEvent(
                        ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            "/bingo timelimit"
                        )
                    )
                }
                .let { yield(it) }
        }

        text.string(StringKey.OptionsCardDifficulty)
            .append(": ")
            .append(
                TierLabel.entries
                    .mapIndexed { index, label ->
                        Text.literal(card.options.itemDistribution.getOrNull(index).toString())
                            .formatted(label.formatting)
                    }
                    .joinText(Text.literal(" "))
            )
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo difficulty"))
            }
            .let { yield(it) }

        text.string(StringKey.OptionsFilter)
            .append(": ")
            .append(objectiveFilterService.formatFilter(card.options.itemFilter, false).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/bingo filter"))
            }
            .let { yield(it) }
    }
}