package app.meetacy.database.updater.auto

import org.jetbrains.exposed.sql.ColumnType

internal object NoOpColumnType : ColumnType() {
    override fun sqlType(): String = error("No operation")
}
