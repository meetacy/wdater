package app.meetacy.database.updater.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun <T> once(
    block: () -> T
): () -> T {
    val lazy = lazy(block)
    return { lazy.value }
}

private val NULL = Any()

@Suppress("UNCHECKED_CAST")
internal fun <T> suspendOnce(
    block: suspend () -> T
): suspend () -> T {
    val mutex = Mutex()
    var initializedValue: Any? = NULL

    return lambda@{
        val value = initializedValue
        if (value !== NULL) return@lambda value as T
        mutex.withLock {
            if (initializedValue !== NULL) return@withLock initializedValue as T
            block().also { value -> initializedValue = value }
        }
    }
}
