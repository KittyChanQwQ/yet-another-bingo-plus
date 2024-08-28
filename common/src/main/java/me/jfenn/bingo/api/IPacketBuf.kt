package me.jfenn.bingo.api

import me.jfenn.bingo.api.item.IItemStack
import net.minecraft.text.Text

interface IPacketBuf {
    fun writeString(str: String)
    fun readString(): String

    fun writeInt(int: Int)
    fun readInt(): Int

    fun writeLong(long: Long)
    fun readLong(): Long

    fun writeFloat(float: Float)
    fun readFloat(): Float

    fun writeBoolean(bool: Boolean)
    fun readBoolean(): Boolean

    fun writeItemStack(stack: IItemStack)
    fun readItemStack(): IItemStack

    fun writeText(text: Text)
    fun readText(): Text

    fun writeByteArray(array: ByteArray)
    fun readByteArray(): ByteArray

    fun <T> writeNullable(value: T?, callback: (T) -> Unit) {
        writeBoolean(value != null)
        value?.let(callback)
    }

    fun <T> readNullable(callback: () -> T): T? {
        return if (readBoolean()) callback() else null
    }

}