package me.jfenn.bingo.api

import java.util.concurrent.ExecutorService

interface IExecutors {
    val main: ExecutorService
    val io: ExecutorService
}