package app.meetacy.database.updater.auto

import org.jetbrains.exposed.sql.Table

internal fun Table.dropColumnStatement(columnName: String): List<String> {
    val column = Table(tableName).registerColumn<Nothing>(columnName, NoOpColumnType)
    return column.dropStatement()
}

internal fun Table.createColumnStatement(columnName: String): List<String> {
    val column = Table(tableName).registerColumn<Nothing>(columnName, NoOpColumnType)
    return column.createStatement()
}
