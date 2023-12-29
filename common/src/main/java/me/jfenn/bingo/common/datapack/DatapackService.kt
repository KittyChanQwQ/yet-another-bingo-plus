package me.jfenn.bingo.common.datapack

import org.apache.commons.io.IOUtils
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

object DatapackService {

    fun copyDataPack(toPath: Path) {
        toPath.parent.toFile().mkdirs()

        Files.deleteIfExists(toPath)
        Files.createFile(toPath)

        this::class.java.getResourceAsStream("/bingo/datapack.zip").use { input ->
            FileOutputStream(toPath.toFile()).use { output ->
                IOUtils.copy(input, output)
            }
        }
    }

}