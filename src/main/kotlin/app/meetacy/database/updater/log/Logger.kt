package app.meetacy.database.updater.log

import java.text.SimpleDateFormat
import java.util.*

public interface Logger {
    public fun log(message: String = "")

    public operator fun get(tag: String): Logger

    public class Simple(
        private val includeTimestamp: Boolean = true,
        private val tag: String? = null,
        private val delimiter: String = " > "
    ) : Logger {
        override fun log(message: String) {
            simpleLog(message, includeTimestamp, tag)
        }

        override fun get(tag: String): Logger {
            this.tag ?: return Simple(includeTimestamp, tag, delimiter)
            return Simple(includeTimestamp, tag = "${this.tag}$delimiter$tag", delimiter)
        }
    }

    public object None : Logger {
        override fun get(tag: String): None = this

        override fun log(message: String) {}
    }
}

private val format = SimpleDateFormat("dd-MM-yyyy/hh:mm:ss")

private fun simpleLog(
    message: String,
    includeTimestamp: Boolean,
    tag: String?
) {
    val prettyDate = format.format(Date())

    val log = buildString {
        if (includeTimestamp) {
            append(prettyDate)
        }
        if (tag != null) {
            append(" [$tag]")
        }
        if (includeTimestamp || tag != null) {
            append(": ")
        }
        append(message)
    }

    println(log)
}
