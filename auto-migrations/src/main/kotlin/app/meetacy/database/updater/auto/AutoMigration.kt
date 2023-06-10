package app.meetacy.database.updater.auto

import app.meetacy.database.updater.Migration
import app.meetacy.database.updater.MigrationContext
import org.jetbrains.exposed.sql.Table

public fun AutoMigration(
    vararg tables: Table,
    fromVersion: Int,
    toVersion: Int = fromVersion + 1
): AutoMigration = AutoMigration(tables.toList(), fromVersion, toVersion)

public class AutoMigration(
    private val tables: List<Table>,
    override val fromVersion: Int,
    override val toVersion: Int = fromVersion + 1
) : Migration {

    override suspend fun MigrationContext.migrate() {
        logger.log("Trying to auto migrate from current state")
        val tablesOnServer = transaction.db.dialect.allTablesNames()
        removeObsoleteTables(tables, tablesOnServer)
    }

    private fun MigrationContext.removeObsoleteTables(
        schemaTables: List<Table>,
        actualNames: List<String>
    ) {
        val logger = logger["remove-obsolete-tables"]

        val obsoleteTables = actualNames
            .filter { name ->
                schemaTables.none { table -> table.tableName == name }
            }

        logger.log("Found obsolete tables: $obsoleteTables")

        val drop = obsoleteTables.flatMap { tableName ->
            Table(tableName).dropStatement()
        }
        transaction.execInBatch(drop)

        logger.log("Obsolete tables dropped")
    }

    override val displayName: String = "AutoMigration"
}
