package me.jfenn.bingo.platform.event.model

sealed class ActionResult<T>{
    abstract val value: T

    class Fail<T>(override val value: T) : me.jfenn.bingo.platform.event.model.ActionResult<T>()
    class Success<T>(override val value: T) : me.jfenn.bingo.platform.event.model.ActionResult<T>()
    class Pass<T>(override val value: T) : me.jfenn.bingo.platform.event.model.ActionResult<T>()

    fun <R> map(fn: (T) -> R): me.jfenn.bingo.platform.event.model.ActionResult<R> {
        return when (this) {
            is me.jfenn.bingo.platform.event.model.ActionResult.Fail -> me.jfenn.bingo.platform.event.model.ActionResult.Fail(
                fn(value)
            )
            is me.jfenn.bingo.platform.event.model.ActionResult.Success -> me.jfenn.bingo.platform.event.model.ActionResult.Success(
                fn(value)
            )
            is me.jfenn.bingo.platform.event.model.ActionResult.Pass -> me.jfenn.bingo.platform.event.model.ActionResult.Pass(
                fn(value)
            )
        }
    }

    companion object {
        fun <T> collapse(results: List<me.jfenn.bingo.platform.event.model.ActionResult<T>?>): me.jfenn.bingo.platform.event.model.ActionResult<T>? {
            results.find { it is me.jfenn.bingo.platform.event.model.ActionResult.Fail<T> }
                ?.let { return it }

            results.find { it is me.jfenn.bingo.platform.event.model.ActionResult.Success<T> }
                ?.let { return it }

            // Otherwise, return a PASS result (preferring a returned value)
            return results.find { it is me.jfenn.bingo.platform.event.model.ActionResult.Pass<T> }
        }
    }
}