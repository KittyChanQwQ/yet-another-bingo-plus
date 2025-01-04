package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.commands.ISignedMessage
import net.minecraft.network.message.SignedMessage
import net.minecraft.text.Text
import java.util.*

class SignedMessageImpl(
    val message: SignedMessage,
) : ISignedMessage {
    override val sender: UUID
        get() = message.sender
    override val text: Text
        get() = message.content
    override val raw: String
        get() = message.signedContent

    override fun withUnsignedContent(text: Text): ISignedMessage {
        return SignedMessageImpl(message.withUnsignedContent(text))
    }
}