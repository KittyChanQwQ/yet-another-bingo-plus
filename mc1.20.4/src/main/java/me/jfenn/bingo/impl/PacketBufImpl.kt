package me.jfenn.bingo.impl

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.api.IPacketBuf
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text

class PacketBufImpl(
    private val buf: PacketByteBuf,
    private val itemStackFactory: IItemStackFactory,
) : IPacketBuf {
    override fun writeString(str: String) = buf.writeString(str).let {}
    override fun readString(): String = buf.readString()

    override fun writeInt(int: Int) = buf.writeInt(int).let {}
    override fun readInt(): Int = buf.readInt()

    override fun writeLong(long: Long) = buf.writeLong(long).let {}
    override fun readLong(): Long = buf.readLong()

    override fun writeBoolean(bool: Boolean) = buf.writeBoolean(bool).let {}
    override fun readBoolean(): Boolean = buf.readBoolean()

    override fun writeItemStack(stack: IItemStack) = buf.writeItemStack(stack.stack).let {}
    override fun readItemStack(): IItemStack = itemStackFactory.forStack(buf.readItemStack())

    override fun writeText(text: Text) = buf.writeText(text).let {}
    override fun readText(): Text = buf.readText()
}
