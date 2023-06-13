package app.meetacy.database.updater.auto

internal data class TableName(
    val schema: Schema,
    val name: String
) {
    sealed interface Schema {
        object Default : Schema
        data class Custom(val string: String) : Schema
    }
}
