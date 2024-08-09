package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IExecutors
import net.minecraft.util.Util
import java.util.concurrent.ExecutorService

object Executors : IExecutors {
    override val main: ExecutorService = Util.getMainWorkerExecutor()
    override val io: ExecutorService = Util.getIoWorkerExecutor()
}
