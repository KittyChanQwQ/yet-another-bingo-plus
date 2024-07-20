package me.jfenn.bingo.api.commands

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.koin.core.scope.Scope

class CommandBuilder(
    private val parent: CommandNode
) {

    fun literal(name: String, configure: CommandBuilder.() -> Unit) {
        val node = CommandNode.Literal(name)
        CommandBuilder(node).configure()
        parent.children += node
    }

    fun string(
        name: String,
        suggestions: (IExecutionContext.() -> List<String>)?,
        configure: CommandBuilder.(arg: CommandArgument<String>) -> Unit
    ) {
        val arg = CommandArgument.String(name, suggestions)
        val node = CommandNode.RequiredArgument(arg)
        CommandBuilder(node).configure(arg)
        parent.children += node
    }

    fun text(name: String, configure: CommandBuilder.(arg: CommandArgument<Text>) -> Unit) {
        val arg = CommandArgument.Text(name)
        val node = CommandNode.RequiredArgument(arg)
        CommandBuilder(node).configure(arg)
        parent.children += node
    }

    fun integer(
        name: String,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        configure: CommandBuilder.(arg: CommandArgument<Int>) -> Unit
    ) {
        val arg = CommandArgument.Integer(name, min, max)
        val node = CommandNode.RequiredArgument(arg)
        CommandBuilder(node).configure(arg)
        parent.children += node
    }

    fun player(name: String, configure: CommandBuilder.(arg: CommandArgument<ServerPlayerEntity>) -> Unit) {
        val arg = CommandArgument.Player(name)
        val node = CommandNode.RequiredArgument(arg)
        CommandBuilder(node).configure(arg)
        parent.children += node
    }

    fun requires(callback: IExecutionSource.() -> Boolean) {
        parent.requires = callback
    }

    fun executes(callback: IExecutionContext.() -> Unit) {
        parent.callback = callback
    }
}

sealed class CommandNode {
    val children = mutableListOf<CommandNode>()
    var requires: (IExecutionSource.() -> Boolean)? = null
    var callback: (IExecutionContext.() -> Unit)? = null

    class Root : CommandNode()
    class Literal(val name: String) : CommandNode()
    class RequiredArgument<T>(val arg: CommandArgument<T>) : CommandNode()
}

sealed class CommandArgument<T> {
    abstract val name: kotlin.String

    class String(
        override val name: kotlin.String,
        val suggestions: (IExecutionContext.() -> List<kotlin.String>)?,
    ) : CommandArgument<kotlin.String>()

    class Text(
        override val name: kotlin.String,
    ) : CommandArgument<net.minecraft.text.Text>()

    class Integer(
        override val name: kotlin.String,
        val min: Int,
        val max: Int,
    ) : CommandArgument<Int>()

    class Player(
        override val name: kotlin.String,
    ) : CommandArgument<ServerPlayerEntity>()
}

interface IExecutionSource {
    val server: MinecraftServer
    val scope: Scope
    val player: ServerPlayerEntity?
}

interface IExecutionContext : IExecutionSource {
    fun sendMessage(text: Text)
    fun sendFeedback(text: Text)
    fun <T> getArgument(arg: CommandArgument<T>): T
}
