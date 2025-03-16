package me.jfenn.bingo.platform

import kotlinx.datetime.Instant
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.platform.item.IItemStack
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    fun writeDuration(value: Duration) {
        writeLong(value.inWholeMilliseconds)
    }

    fun readDuration(): Duration {
        return readLong().milliseconds
    }

    fun writeInstant(value: Instant) {
        writeLong(value.toEpochMilliseconds())
    }

    fun readInstant(): Instant {
        return Instant.fromEpochMilliseconds(readLong())
    }

    fun writeUUID(value: UUID) {
        writeLong(value.mostSignificantBits)
        writeLong(value.leastSignificantBits)
    }

    fun readUUID(): UUID {
        return UUID(readLong(), readLong())
    }

    fun <T: Enum<T>> writeEnum(value: T) {
        writeInt(value.ordinal)
    }
}

inline fun <reified T: Enum<T>> IPacketBuf.readEnum(): T {
    val ordinal = readInt()
    val values = enumValues<T>()
    return values.getOrElse(ordinal) { values.first() }
}
