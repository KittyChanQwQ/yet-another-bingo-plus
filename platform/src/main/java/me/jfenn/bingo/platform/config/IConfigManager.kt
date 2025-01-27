package me.jfenn.bingo.platform.config

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

interface IConfigManager {
    val configDir: Path
    fun inputStream(path: String): InputStream
    fun outputStream(path: String): OutputStream
}