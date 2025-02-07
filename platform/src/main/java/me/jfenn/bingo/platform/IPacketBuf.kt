package me.jfenn.bingo.platform

import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.platform.item.IItemStack

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

    fun writeText(text: IText?)
    fun readText(): IText

    fun writeByteArray(array: ByteArray)
    fun readByteArray(): ByteArray

    fun <T> writeNullable(value: T?, callback: (T) -> Unit) {
        writeBoolean(value != null)
        value?.let(callback)
    }

    fun <T> readNullable(callback: () -> T): T? {
        return if (readBoolean()) callback() else null
    }

    fun <T> writeList(value: Collection<T>, callback: (T) -> Unit) {
        val list = value.toList()
        writeInt(list.size)
        list.forEach(callback)
    }

    fun <T> readList(callback: () -> T): List<T> {
        return List(readInt()) { callback() }
    }
}