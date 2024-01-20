package me.jfenn.bingo.common.commands

class CommandManager {
    init {
        BingoCommands.init()
        JoinCommand.init()
        ChatCommand.init()
        CoordsCommand.init()
        RestartCommand.init()
    }
}