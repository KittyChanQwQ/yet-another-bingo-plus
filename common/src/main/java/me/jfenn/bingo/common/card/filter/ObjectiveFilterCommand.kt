package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.api.commands.IExecutionContext
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.card.tag.TagService
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.commands.hasState
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class ObjectiveFilterCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
    private val config: BingoConfig,
) {
    private fun IExecutionContext.setItemFilters(mutate: (ObjectiveFilterList) -> ObjectiveFilterList) {
        val events = scope.get<ScopedEvents>()
        val cardService = scope.get<CardService>()
        val cardViewService = scope.get<CardViewService>()
        val card = cardViewService.getPlayerCard(player!!)

        card.options.itemFilter = mutate(card.options.itemFilter)

        events.onChangeOptions(Unit)
        cardService.generateCard()
        Text.literal("ℹ  ")
            .append(text.string(StringKey.OptionsFilter))
            .append(": ")
            .append(card.options.formatFilter(config))
            .formatted(Formatting.AQUA)
            .also { sendFeedback(it) }
    }

    private fun IExecutionContext.listTags(): List<String> {
        return scope.get<TagService>().getTags().keys.toList()
    }

    private fun IExecutionContext.listPresets(): List<String> {
        return scope.get<ObjectiveFilterService>().getPresetFilters().keys.toList()
    }

    init {
        commandManager.register("bingo") {
            literal("filter") {
                requires {
                    hasState(GameState.PREGAME, GameState.PLAYING) && hasPermission(Permission.CONFIGURE_GAME)
                }

                literal("preset") {
                    string("preset", { listPresets() }) { presetArg ->
                        executes {
                            val presetName = getArgument(presetArg)
                            val preset = scope.get<ObjectiveFilterService>()
                                .getPresetFilters()[presetName]!!

                            setItemFilters { preset }
                        }
                    }
                }

                literal("only") {
                    string("tag", { listTags() }) { tagArg ->
                        executes {
                            val tag = getArgument(tagArg)
                            require(tag in listTags()) { "Tag '$tag' does not exist!" }
                            setItemFilters {
                                it.plus(ObjectiveFilter.Include(tag))
                            }
                        }
                    }
                }

                literal("exclude") {
                    string("tag", { listTags() }) { tagArg ->
                        executes {
                            val tag = getArgument(tagArg)
                            require(tag in listTags()) { "Tag '$tag' does not exist!" }
                            setItemFilters {
                                it.plus(ObjectiveFilter.Exclude(tag))
                            }
                        }
                    }
                }

                literal("count") {
                    string("tag", { listTags() }) { tagArg ->
                        integer("count", min = 1, max = 25) { countArg ->
                            executes {
                                val tag = getArgument(tagArg)
                                val count = getArgument(countArg)
                                require(tag in listTags()) { "Tag '$tag' does not exist!" }
                                setItemFilters {
                                    it.plus(ObjectiveFilter.Count(tag, count))
                                }
                            }
                        }
                    }
                }

                literal("clear") {
                    executes {
                        setItemFilters { ObjectiveFilterList() }
                    }
                }
            }
        }
    }
}