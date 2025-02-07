package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.filter.ObjectiveFilterPreset
import me.jfenn.bingo.common.card.objective.ObjectiveListService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.commands.ICommandManager

class DataCommands(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {
    init {
        commandManager.register("bingodata") {
            requires {
                hasPermission(Permission.COMMAND_DATA)
            }

            literal("tag") {
                literal("add") {
                    string(
                        name = "tag",
                        suggestions = { scope.get<ScopedData>().tags.keys },
                    ) { tagArg ->
                        string(
                            name = "objective",
                            suggestions = { scope.get<ObjectiveListService>().getAllObjectives() },
                            greedy = true,
                        ) { objectiveArg ->
                            executes {
                                val tag = getArgument(tagArg)
                                val objective = getArgument(objectiveArg)
                                scope.get<DataCommandService>().addToTag(
                                    tagName = tag,
                                    objective = objective,
                                )
                                sendFeedback(
                                    text.string(
                                        StringKey.CommandDataTagAddSuccess,
                                        objective,
                                        ObjectiveFilterPreset.formatName(text, tag),
                                    )
                                )
                            }
                        }
                    }
                }

                literal("remove") {
                    string(
                        name = "tag",
                        suggestions = { scope.get<ScopedData>().tags.keys },
                    ) { tagArg ->
                        string(
                            name = "objective",
                            suggestions = { scope.get<ObjectiveListService>().getAllObjectives() },
                            greedy = true,
                        ) { objectiveArg ->
                            executes {
                                val tag = getArgument(tagArg)
                                val objective = getArgument(objectiveArg)
                                scope.get<DataCommandService>().removeFromTag(
                                    tagName = tag,
                                    objective = objective,
                                )
                                sendFeedback(
                                    text.string(
                                        StringKey.CommandDataTagRemoveSuccess,
                                        objective,
                                        ObjectiveFilterPreset.formatName(text, tag),
                                    )
                                )
                            }
                        }
                    }
                }
            }

            literal("tierlist") {
                literal("add") {
                    string(
                        name = "list",
                        suggestions = { scope.get<ScopedData>().tierLists.keys },
                    ) { listArg ->
                        for (tier in TierLabel.entries + null) {
                            literal(tier?.name?.lowercase() ?: "uncategorized") {
                                string(
                                    name = "objective",
                                    suggestions = { scope.get<ObjectiveListService>().getAllObjectives() },
                                    greedy = true,
                                ) { objectiveArg ->
                                    executes {
                                        val list = getArgument(listArg)
                                        val objective = getArgument(objectiveArg)
                                        scope.get<DataCommandService>().addToTierList(
                                            listName = list,
                                            objective = objective,
                                            tier = tier,
                                        )
                                        sendFeedback(
                                            text.string(
                                                StringKey.CommandDataTierlistAddSuccess,
                                                objective,
                                                tier?.string ?: StringKey.ListUncategorized,
                                                ObjectiveFilterPreset.formatName(text, list),
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                literal("remove") {
                    string(
                        name = "list",
                        suggestions = { scope.get<ScopedData>().tierLists.keys },
                    ) { listArg ->
                        string(
                            name = "objective",
                            suggestions = { scope.get<ObjectiveListService>().getAllObjectives() },
                            greedy = true,
                        ) { objectiveArg ->
                            executes {
                                val list = getArgument(listArg)
                                val objective = getArgument(objectiveArg)
                                scope.get<DataCommandService>().removeFromTierList(
                                    listName = list,
                                    objective = objective,
                                )
                                sendFeedback(
                                    text.string(
                                        StringKey.CommandDataTierlistRemoveSuccess,
                                        objective,
                                        ObjectiveFilterPreset.formatName(text, list),
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}