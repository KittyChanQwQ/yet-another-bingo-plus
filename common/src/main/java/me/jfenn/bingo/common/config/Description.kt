package me.jfenn.bingo.common.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Description(val description: String)
