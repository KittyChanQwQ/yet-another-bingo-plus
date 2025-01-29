package me.jfenn.bingo.platform.config

import kotlinx.datetime.Instant
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.reflect.KClass

interface IConfigManager {
    val configDir: Path

    fun inputStream(path: String): InputStream
    fun outputStream(path: String): OutputStream

    /**
     * Returns the last modified timestamp if the file exists on disk
     * Otherwise, returns DISTANT_PAST
     */
    fun readLastModified(fileName: String): Instant
    fun <T: Any> read(clazz: KClass<T>, file: String): T
    fun <T: Any> write(clazz: KClass<T>, file: String, config: T)
}