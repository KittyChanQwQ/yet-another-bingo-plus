package me.jfenn.bingo.common.ready

import me.jfenn.bingo.platform.commands.ICommandManager

class ReadyCommand(
    commandManager: ICommandManager,
) {
    init {
        commandManager.register("ready") {
            requires {
                scope.get<ReadyService>().canUseReady(player!!)
            }
            executes {
                scope.get<ReadyService>().toggleReady(player!!)
            }
        }
    }
}