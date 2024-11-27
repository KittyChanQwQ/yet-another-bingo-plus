package me.jfenn.bingo.impl.networking

import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.IPacketBuf
import net.minecraft.item.ItemStack
import net.minecraft.network.RegistryByteBuf
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs

class PacketBufImpl(
    private val buf: RegistryByteBuf,
    private val itemStackFactory: IItemStackFactory,
) : IPacketBuf {
    override fun writeString(str: String) = buf.writeString(str).let {}
    override fun readString(): String = buf.readString()

    override fun writeInt(int: Int) = buf.writeInt(int).let {}
    override fun readInt(): Int = buf.readInt()

    override fun writeLong(long: Long) = buf.writeLong(long).let {}
    override fun readLong(): Long = buf.readLong()

    override fun writeFloat(float: Float) = buf.writeFloat(float).let {}
    override fun readFloat(): Float = buf.readFloat()

    override fun writeBoolean(bool: Boolean) = buf.writeBoolean(bool).let {}
    override fun readBoolean(): Boolean = buf.readBoolean()

    override fun writeItemStack(stack: IItemStack) = ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, stack.stack)
    override fun readItemStack(): IItemStack = itemStackFactory.forStack(ItemStack.OPTIONAL_PACKET_CODEC.decode(buf))

    override fun writeText(text: Text) = TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, text)
    override fun readText(): Text = TextCodecs.REGISTRY_PACKET_CODEC.decode(buf)

    override fun writeByteArray(array: ByteArray) = buf.writeByteArray(array).let {}
    override fun readByteArray(): ByteArray = buf.readByteArray()
}
