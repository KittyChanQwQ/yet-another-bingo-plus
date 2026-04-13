package me.jfenn.bingo.platform

@JvmInline
value class Identifier(
    val id: String,
) {
    companion object {
        fun of(namespace: String, path: String) = Identifier("$namespace:$path")
    }

    val namespace get() = id.substringBefore(':')
    val path get() = id.substringAfter(':')

    override fun toString(): String = id
}
