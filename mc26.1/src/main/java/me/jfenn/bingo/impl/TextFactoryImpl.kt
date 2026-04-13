package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.text.*
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.network.chat.contents.objects.PlayerSprite
import net.minecraft.world.item.component.ResolvableProfile
import java.util.*

class TextFactoryImpl : ITextFactory {
    override fun empty(): IText {
        return TextImpl(Component.empty())
    }

    override fun from(text: Component): IText {
        return TextImpl(text as? MutableComponent ?: text.copy())
    }

    override fun literal(str: String): IText {
        return TextImpl(Component.literal(str))
    }

    override fun keybind(key: String): IText {
        return TextImpl(Component.keybind(key))
    }

    override fun translatable(key: String, fallback: String?, vararg args: Any): IText {
        val mappedArgs = args.map {
            when (it) {
                is IText -> it.value
                else -> it
            }
        }
        return TextImpl(Component.translatableWithFallback(key, fallback, *mappedArgs.toTypedArray()))
    }

    override fun bracketedCopyable(str: String): IText {
        return TextImpl(ComponentUtils.copyOnClickText(str))
    }

    override fun player(uuid: UUID): IText {
        return TextImpl(
            Component.`object`(
                PlayerSprite(
                    ResolvableProfile.createUnresolved(uuid),
                    true
                )
            )
        )
    }
}

class TextImpl(
    override val value: MutableComponent
) : IText {
    override fun isEmpty(): Boolean {
        return value.string.isEmpty()
    }

    override fun setClickEvent(event: TextAction) {
        value.style = value.style.withClickEvent(
            when (event) {
                is TextAction.OpenUrl -> ClickEvent.OpenUrl(event.url)
                is TextAction.RunCommand -> ClickEvent.RunCommand(event.command)
                is TextAction.SuggestCommand -> ClickEvent.SuggestCommand(event.command)
            }
        )
    }

    override fun setHoverEvent(event: HoverAction) {
        value.setStyle(
            value.style.withHoverEvent(
                when (event) {
                    is HoverAction.ShowText -> {
                        val text = event.text
                        require(text is TextImpl)
                        HoverEvent.ShowText(text.value)
                    }
                }
            )
        )
    }

    override fun resetStyle(): IText {
        value.setStyle(Style.EMPTY.withItalic(false))
        return this
    }

    override fun bracketed(): IText {
        return TextImpl(ComponentUtils.wrapInSquareBrackets(value))
    }

    override fun formatted(vararg formatting: ChatFormatting): IText {
        value.withStyle(*formatting)
        return this
    }

    override fun formatted(vararg formatting: Formatting): IText {
        value.withStyle(*formatting.map { it.toMinecraftFormatting() }.toTypedArray())
        return this
    }

    override fun setColor(color: Int) {
        value.withStyle { it.withColor(color) }
    }

    override fun append(text: IText): IText {
        require(text is TextImpl)
        value.append(text.value)
        return this
    }

    override fun append(str: String): IText {
        value.append(str)
        return this
    }

    override fun equals(other: Any?): Boolean = other is TextImpl && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value.string

    override fun copy() = TextImpl(value.copy())
}