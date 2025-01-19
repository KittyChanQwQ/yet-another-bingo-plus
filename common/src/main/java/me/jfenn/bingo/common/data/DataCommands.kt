package me.jfenn.bingo.common.data

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.objective.ObjectiveListService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.commands.hasPermission
import me.jfenn.bingo.platform.commands.ICommandManager

class DataCommands(
    commandManager: ICommandManager,
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
                                scope.get<DataCommandService>().addToTag(
                                    tagName = getArgument(tagArg),
                                    objective = getArgument(objectiveArg),
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
                                scope.get<DataCommandService>().removeFromTag(
                                    tagName = getArgument(tagArg),
                                    objective = getArgument(objectiveArg),
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
                            literal(tier?.name ?: "uncategorized") {
                                string(
                                    name = "objective",
                                    suggestions = { scope.get<ObjectiveListService>().getAllObjectives() },
                                    greedy = true,
                                ) { objectiveArg ->
                                    executes {
                                        scope.get<DataCommandService>().addToTierList(
                                            listName = getArgument(listArg),
                                            objective = getArgument(objectiveArg),
                                            tier = tier,
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
                                scope.get<DataCommandService>().removeFromTierList(
                                    listName = getArgument(listArg),
                                    objective = getArgument(objectiveArg),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}