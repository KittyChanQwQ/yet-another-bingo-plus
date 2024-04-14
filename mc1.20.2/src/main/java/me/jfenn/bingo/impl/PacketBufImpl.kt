package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IPacketBuf
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text

class PacketBufImpl(
    private val buf: PacketByteBuf
) : IPacketBuf {
    override fun writeString(str: String) = buf.writeString(str).let {}
    override fun readString(): String = buf.readString()

    override fun writeInt(int: Int) = buf.writeInt(int).let {}
    override fun readInt(): Int = buf.readInt()

    override fun writeLong(long: Long) = buf.writeLong(long).let {}
    override fun readLong(): Long = buf.readLong()

    override fun writeBoolean(bool: Boolean) = buf.writeBoolean(bool).let {}
    override fun readBoolean(): Boolean = buf.readBoolean()

    override fun writeItemStack(stack: ItemStack) = buf.writeItemStack(stack).let {}
    override fun readItemStack(): ItemStack = buf.readItemStack()

    override fun writeText(text: Text) = buf.writeText(text).let {}
    override fun readText(): Text = buf.readText()
}
