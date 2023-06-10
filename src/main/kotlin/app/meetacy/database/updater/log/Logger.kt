package app.meetacy.database.updater.log

import java.text.SimpleDateFormat
import java.util.*

public interface Logger {
    public fun log(message: String = "")

    public operator fun get(tag: String): Logger

    public class Simple(private val tag: String? = null) : Logger {
        override fun log(message: String) {
            simpleLog(message, tag)
        }

        override fun get(tag: String): Logger {
            this.tag ?: return Simple(tag)
            return Simple(tag = "${this.tag} > $tag")
        }
    }

    public object None : Logger {
        override fun get(tag: String): None = this

        override fun log(message: String) {}
    }
}

private val format = SimpleDateFormat("dd-MM-yyyy/hh:mm:ss")

private fun simpleLog(message: String, tag: String?) {
    val prettyDate = format.format(Date())

    if (tag == null) {
        println("$prettyDate: $message")
    } else {
        println("$prettyDate [$tag]: $message")
    }
}
