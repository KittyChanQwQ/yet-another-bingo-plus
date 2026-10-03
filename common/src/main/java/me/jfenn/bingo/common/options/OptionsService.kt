package me.jfenn.bingo.common.options

import me.jfenn.bingo.api.options.*
import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.filter.ObjectiveFilterService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.commands.BingoOptionsCommands
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.controller.GameRuleController
import me.jfenn.bingo.common.event.model.OptionsChangedEvent
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.spawn.ElytraService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.minutes
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.text.TextAction
import java.time.Duration
import java.util.*
import kotlin.reflect.KMutableProperty1

internal val IExecutionContext.optionsContext
    get() = OptionsService.Context(
        player = player,
        receiveFeedback = ::sendFeedback,
        receiveError = ::error,
    )

internal inline fun withContext(ctx: OptionsService.Context, callback: () -> Unit) {
    try {
        OptionsService.localCtx.set(ctx)
        callback()
    } finally {
        OptionsService.localCtx.remove()
    }
}

internal inline fun IExecutionContext.withOptionsService(callback: (OptionsService) -> Unit) {
    withContext(optionsContext) {
        callback(scope.get<OptionsService>())
    }
}

internal class OptionsService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val cardService: CardService,
    private val configService: ConfigService,
    private val cardViewService: CardViewService,
    private val elytraService: ElytraService,
    private val objectiveFilterService: ObjectiveFilterService,
    private val teamService: TeamService,
    private val playerManager: IPlayerManager,
    private val text: TextProvider,
    private val eventBus: IEventBus,
) : IOptionsService {
    companion object {
        val localCtx = ThreadLocal<Context?>()
        val fallbackCtx = Context(null)
        val ctx get() = localCtx.get() ?: fallbackCtx
    }

    data class Context(
        val player: IPlayerHandle?,
        val receiveFeedback: ((IText) -> Unit)? = null,
        val receiveError: ((IText) -> Nothing)? = null,
    )

    fun broadcastHotbarMessage(player: IPlayerHandle, message: IText) {
        playerManager.getPlayers()
            .filterNot { it.uuid == player.uuid }
            .forEach {
                it.sendHotbarMessage(
                    text.translatable("chat.type.admin", null, player.playerName, message)
                        .formatted(Formatting.GRAY, Formatting.ITALIC)
                )
            }
    }

    fun Context.sendFeedback(message: IText) {
        receiveFeedback?.invoke(message)
            ?: player?.let { player ->
                broadcastHotbarMessage(player, message)
            }
    }

    fun Context.error(message: IText) {
        receiveError?.invoke(message)
        throw IllegalArgumentException(message.toString())
    }

    fun setGoal(
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
            descriptive = true,
        )
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsGoal, goalName.formatted(Formatting.WHITE))
        )
        eventBus.emit(OptionsChangedEvent)
    }

    fun setCardDifficulty(
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
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsCardDifficulty, card.options.formatItemDist(text))
        )
        eventBus.emit(OptionsChangedEvent)
    }

    fun setCardFilter(
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
        eventBus.emit(OptionsChangedEvent)
    }

    fun toggleCardMode(
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
                text.joinText(
                    options.formatGameMode(card)
                        .map { text.string(it) }
                ).formatted(Formatting.WHITE),
            )
        )
        eventBus.emit(OptionsChangedEvent)
    }

    override var winCondition: BingoWinCondition
        get() = options.winCondition
        set(value) = setWinConditionInternal(value)

    private fun setWinConditionInternal(
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
        eventBus.emit(OptionsChangedEvent, Unit)
    }

    override var stalemateBehavior: StalemateBehavior
        get() = options.stalemateBehavior
        set(value) = setStalemateBehaviorInternal(value)

    private fun setStalemateBehaviorInternal(
        stalemateBehavior: StalemateBehavior,
    ) {
        options.stalemateBehavior = stalemateBehavior

        // Send a message that the goal was changed
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsWinBehaviorWhenStalemate, text.string(stalemateBehavior.string).formatted(Formatting.WHITE))
        )
        eventBus.emit(OptionsChangedEvent, Unit)
    }

    override var endGameWhen: EndWhen
        get() = options.endGameWhen
        set(value) = setEndWhen(value)

    private fun setEndWhen(
        endWhen: EndWhen,
    ) {
        options.endGameWhen = endWhen
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsWinBehaviorEndWhen, endWhen.string(text).formatted(Formatting.WHITE))
        )
        eventBus.emit(OptionsChangedEvent, Unit)
    }

    override var isKeepInventory: Boolean
        get() = options.isKeepInventory
        set(value) = toggleKeepInventory(value)

    fun toggleKeepInventory(
        isKeepInventory: Boolean = !options.isKeepInventory,
    ) {
        options.isKeepInventory = isKeepInventory
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsKeepInventory, text.boolean(isKeepInventory))
        )
        eventBus.emit(GameRuleController.GameRuleChangedEvent)
        eventBus.emit(OptionsChangedEvent)
    }

    override var showPreviewCard: Boolean
        get() = options.showPreviewCard
        set(value) = togglePreviewCard(value)

    fun togglePreviewCard(
        showPreviewCard: Boolean? = null,
    ) {
        options.showPreviewCard = showPreviewCard ?: !options.showPreviewCard
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsPreviewCard, text.boolean(options.showPreviewCard))
        )
        eventBus.emit(OptionsChangedEvent)
    }

    override var isElytra: Boolean
        get() = options.isElytra
        set(value) = toggleElytra(value)

    fun toggleElytra(
        isElytra: Boolean? = null,
    ) {
        options.isElytra = isElytra ?: !options.isElytra

        // if Elytra mode is turned off during a game, remove all players' elytra
        if (!options.isElytra && state.isLobbyMode) {
            for (player in playerManager.getPlayers()) {
                elytraService.takeElytra(player)
            }
        }

        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsElytra, text.boolean(options.isElytra))
        )
        eventBus.emit(OptionsChangedEvent)
    }

    override var isNightVision: Boolean
        get() = options.isNightVision
        set(value) = toggleNightVision(value)

    fun toggleNightVision(
        isNightVision: Boolean? = null,
    ) {
        options.isNightVision = isNightVision ?: !options.isNightVision

        // if night vision is changed, update all players
        if (state.state == GameState.PLAYING) {
            playerManager.getPlayers().forEach { player ->
                state.playersJoinedIds.add(player.uuid)
            }
        }

        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsNightVis, text.boolean(options.isNightVision))
        )
        eventBus.emit(OptionsChangedEvent)
    }

    override var isPvpEnabled: Boolean
        get() = options.isPvpEnabled
        set(value) = togglePvp(value)

    fun togglePvp(
        isPvpEnabled: Boolean? = null,
    ) {
        options.isPvpEnabled = isPvpEnabled ?: !options.isPvpEnabled
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsAllowPvp, text.boolean(options.isPvpEnabled))
        )
        eventBus.emit(GameRuleController.GameRuleChangedEvent)
        eventBus.emit(OptionsChangedEvent)
    }

    override var isUnlockRecipes: Boolean
        get() = options.isUnlockRecipes
        set(value) = toggleUnlockRecipes(value)

    fun toggleUnlockRecipes(
        isUnlockRecipes: Boolean = !options.isUnlockRecipes,
    ) {
        options.isUnlockRecipes = isUnlockRecipes
        ctx.sendFeedback(
            text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsUnlockRecipes, text.boolean(isUnlockRecipes))
        )
    }

    override var timeLimit: Duration?
        get() = options.timeLimit
        set(value) = setTimeLimit(value?.toMinutes()?.toInt())

    fun setTimeLimit(
        minutes: Int?
    ) {
        // If the game is playing, the command should set the remaining time (add the current time before applying)
        val newTimeLimit = minutes?.takeIf { it > 0 }?.minutes
        val ingameDuration = state.ingameDuration()
        options.timeLimit = newTimeLimit?.plus(ingameDuration ?: Duration.ZERO)

        if (newTimeLimit != null && ingameDuration != null) {
            // When the game resets, apply the specified timeLimit (without the added time)
            state.restoreOptions.add(RestoreTimeLimit(newTimeLimit))
        }

        broadcastTimeLimitChanged(newTimeLimit)
    }

    fun broadcastTimeLimitChanged(timeLimit: Duration?) {
        ctx.sendFeedback(
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsTimeLimit,
                timeLimit?.formatString()
                    ?: text.string(StringKey.OptionsTimeLimitOff)
            )
        )
        eventBus.emit(OptionsChangedEvent, Unit)
    }

    fun createRestoreTimeLimit() {
        state.restoreOptions.add(
            RestoreTimeLimit(
                state.restoreOptions
                    .filterIsInstance<RestoreTimeLimit>()
                    .lastOrNull()
                    ?.timeLimit
                    ?: options.timeLimit
            )
        )
    }

    override var spawnDistance: Int
        get() = options.spawnDistance
        set(value) = setSpawnDistanceInternal(value)

    private fun setSpawnDistanceInternal(distance: Int) {
        options.spawnDistance = distance
        ctx.sendFeedback(
            text.string(
                StringKey.OptionsNotifyChanged,
                StringKey.OptionsSpawnDistance,
                text.string(StringKey.OptionsSpawnDistanceChunks, distance)
            )
        )
        eventBus.emit(OptionsChangedEvent, Unit)
    }

    override var spawnDimension: String
        get() = options.spawnDimension
        set(value) = setSpawnDimensionInternal(value)

    private fun setSpawnDimensionInternal(dimension: String) {
        options.spawnDimension = dimension
        val label = text.literal(dimension.substringAfterLast(':').formatTitle())
        ctx.sendFeedback(text.string(StringKey.OptionsNotifyChanged, StringKey.OptionsSpawnDimension, label))
        eventBus.emit(OptionsChangedEvent)
    }

    override var isPlayerKit: Boolean
        get() = options.isPlayerKit
        set(value) = togglePlayerKit(value)

    fun togglePlayerKit(isPlayerKit: Boolean = !options.isPlayerKit) {
        options.isPlayerKit = isPlayerKit
        cardService.generateCard()
        ctx.sendFeedback(text.string(
            StringKey.OptionsNotifyChanged,
            StringKey.OptionsSpawnKitPlayer,
            text.boolean(options.isPlayerKit)
        ))
        eventBus.emit(OptionsChangedEvent)
    }

    override var isTeamKit: Boolean
        get() = options.isTeamKit
        set(value) = toggleTeamKit(value)

    fun toggleTeamKit(isTeamKit: Boolean = !options.isTeamKit) {
        options.isTeamKit = isTeamKit
        cardService.generateCard()
        ctx.sendFeedback(text.string(
            StringKey.OptionsNotifyChanged,
            StringKey.OptionsSpawnKitTeam,
            text.boolean(options.isTeamKit)
        ))
        eventBus.emit(OptionsChangedEvent)
    }

    fun resetToDefaults() {
        // Reset the menu to the default options
        options.copyFrom(configService.optionsDefault)

        // Reset the active card options to the defaults
        cardService.createInitialCards()

        ctx.sendFeedback(text.string(StringKey.OptionsResetDefaults))
    }

    fun getOptionsSummary(player: IPlayerHandle?): Sequence<IText> = sequence {
        val team = player?.let { teamService.getPlayerTeam(it) }
        val card = cardViewService.getPlayerCard(player)

        text.string(
            StringKey.ScoreboardMode,
            text.joinText(
                options.formatGameMode(card)
                    .map { text.string(it) }
            ).formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .apply {
                setClickEvent(TextAction.SuggestCommand("/bingo mode"))
            }
            .let { yield(it) }

        text.string(
            StringKey.ScoreboardFeatures,
            options.formatFeatures()
                .map { text.string(it) }
                .let { text.joinText(it) }
                .formatted(Formatting.YELLOW)
        )
            .formatted(Formatting.GRAY)
            .let { yield(it) }

        text.string(StringKey.OptionsGoal)
            .append(": ")
            .append(state.formatCardGoalsDescriptive(team, text).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.setClickEvent(TextAction.SuggestCommand("/bingo goal"))
            }
            .let { yield(it) }

        if (options.endGameWhen !is EndWhen.FirstWin) {
            text.string(
                StringKey.OptionsEndWhen,
                options.endGameWhen.string(text).formatted(Formatting.YELLOW)
            )
                .formatted(Formatting.GRAY)
                .also {
                    it.setClickEvent(TextAction.SuggestCommand(BingoOptionsCommands.END_WHEN))
                }
                .let { yield(it) }
        }

        options.timeLimit?.let { timeLimit ->
            text.string(StringKey.OptionsTimeLimit)
                .append(": ")
                .append(
                    timeLimit.toMinutes().let { minutes ->
                        String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
                            .let { text.literal(it).formatted(Formatting.YELLOW) }
                    }
                )
                .formatted(Formatting.GRAY)
                .also {
                    it.setClickEvent(TextAction.SuggestCommand("/bingo timelimit"))
                }
                .let { yield(it) }
        }

        text.string(StringKey.OptionsCardDifficulty)
            .append(": ")
            .append(
                text.joinText(
                    TierLabel.entries
                        .mapIndexed { index, label ->
                            text.literal(card.options.itemDistribution.getOrNull(index).toString())
                                .formatted(label.formatting)
                        }
                )
            )
            .formatted(Formatting.GRAY)
            .also {
                it.setClickEvent(TextAction.SuggestCommand("/bingo difficulty"))
            }
            .let { yield(it) }

        text.string(StringKey.OptionsFilter)
            .append(": ")
            .append(objectiveFilterService.formatFilter(card.options.itemFilter, false).formatted(Formatting.YELLOW))
            .formatted(Formatting.GRAY)
            .also {
                it.setClickEvent(TextAction.SuggestCommand("/bingo filter"))
            }
            .let { yield(it) }
    }
}