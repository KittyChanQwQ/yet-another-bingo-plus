package me.jfenn.bingo.platform.commands

import me.jfenn.bingo.platform.IPlayerHandle
import net.minecraft.server.MinecraftServer
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
        suggestions: (IExecutionContext.() -> Iterable<String>)? = null,
        greedy: Boolean = false,
        configure: CommandBuilder.(arg: CommandArgument<String>) -> Unit
    ) {
        val arg = CommandArgument.String(name, suggestions, greedy)
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

    fun boolean(name: String, configure: CommandBuilder.(arg: CommandArgument<Boolean>) -> Unit) {
        val arg = CommandArgument.Bool(name)
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

    fun long(
        name: String,
        min: Long = Long.MIN_VALUE,
        max: Long = Long.MAX_VALUE,
        configure: CommandBuilder.(arg: CommandArgument<Long>) -> Unit
    ) {
        val arg = CommandArgument.NumberLong(name, min, max)
        val node = CommandNode.RequiredArgument(arg)
        CommandBuilder(node).configure(arg)
        parent.children += node
    }

    fun player(name: String, configure: CommandBuilder.(arg: CommandArgument<IPlayerHandle>) -> Unit) {
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
        val suggestions: (IExecutionContext.() -> Iterable<kotlin.String>)?,
        val greedy: Boolean,
    ) : CommandArgument<kotlin.String>()

    class Text(
        override val name: kotlin.String,
    ) : CommandArgument<net.minecraft.text.Text>()

    class Bool(
        override val name: kotlin.String,
    ) : CommandArgument<Boolean>()

    class Integer(
        override val name: kotlin.String,
        val min: Int,
        val max: Int,
    ) : CommandArgument<Int>()

    class NumberLong(
        override val name: kotlin.String,
        val min: Long,
        val max: Long,
    ) : CommandArgument<Long>()

    class Player(
        override val name: kotlin.String,
    ) : CommandArgument<IPlayerHandle>()
}

interface IExecutionSource {
    val server: MinecraftServer
    val scope: Scope
    val player: IPlayerHandle?
    val isConsole: Boolean
    fun error(text: Text): Nothing

    val playerOrThrow
        get() = player ?: error(Text.translatable("permissions.requires.player"))
}

interface IExecutionContext : IExecutionSource {
    fun sendMessage(text: Text)
    fun sendFeedback(text: Text)
    fun <T> getArgument(arg: CommandArgument<T>): T
}
