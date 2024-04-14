package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.api.ITextSerializer
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.text.Text

class PacketBufImpl(
    private val buf: RegistryByteBuf,
    private val textSerializer: ITextSerializer,
) : IPacketBuf {
    override fun writeString(str: String) = buf.writeString(str).let {}
    override fun readString(): String = buf.readString()

    override fun writeInt(int: Int) = buf.writeInt(int).let {}
    override fun readInt(): Int = buf.readInt()

    override fun writeLong(long: Long) = buf.writeLong(long).let {}
    override fun readLong(): Long = buf.readLong()

    override fun writeBoolean(bool: Boolean) = buf.writeBoolean(bool).let {}
    override fun readBoolean(): Boolean = buf.readBoolean()

    override fun writeItemStack(stack: ItemStack) = ItemStack.PACKET_CODEC.encode(buf, stack)
    override fun readItemStack(): ItemStack = ItemStack.PACKET_CODEC.decode(buf)

    override fun writeText(text: Text) = buf.writeString(textSerializer.toJson(text)).let {}
    override fun readText(): Text = textSerializer.fromJson(buf.readString())
}
