package app.meetacy.database.updater.auto

import app.meetacy.database.updater.Migration
import app.meetacy.database.updater.MigrationContext
import app.meetacy.database.updater.log.Logger
import app.meetacy.database.updater.log.SQL
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.vendors.ColumnMetadata

public fun AutoMigration(
    vararg tables: Table,
    fromVersion: Int,
    toVersion: Int = fromVersion + 1
): AutoMigration = AutoMigration(tables.toList(), fromVersion, toVersion)

/**
 * Auto migrations do not support renaming/deleting
 */
public class AutoMigration(
    private val tables: List<Table>,
    override val fromVersion: Int,
    override val toVersion: Int = fromVersion + 1
) : Migration {

    override suspend fun MigrationContext.migrate() {
        logger.log("Trying to auto migrate from current state")
        migrateTables()
        migrateColumns()
    }

    private fun MigrationContext.migrateTables() {
        logger.log("Migrating tables...")
        val logger = logger["tables-migration"]

        val rawNames = transaction.db.dialect.allTablesNames()
        logger.log("Tables from server: $rawNames")
        val schema = transaction.connection.schema
        logger.log("Default schema: $schema")

        val tablesOnServer = rawNames.map { name -> name.removePrefix("$schema.") }

        createMissingTables(logger, tablesOnServer)
    }

    private fun MigrationContext.createMissingTables(
        baseLogger: Logger,
        tablesOnServer: List<String>
    ) {
        baseLogger.log("Creating missing tables...")
        val logger = baseLogger["create"]

        val newTables = tables.filter { table ->
            tablesOnServer.none { name -> normalizedTableName(table) == name }
        }

        if (newTables.isEmpty()) {
            logger.log("No new tables were found")
            return
        }

        logger.log("New tables were found: $newTables, creating them...")

        val create = SchemaUtils.createStatements(*newTables.toTypedArray())
        execInBatch(logger, create)

        logger.log("New tables were created")
    }

    private fun MigrationContext.migrateColumns() {
        logger.log("Migrating columns...")
        val logger = logger["columns-migration"]
        val columnsOnServer = transaction.db.dialect.tableColumns(*tables.toTypedArray())
        createMissingColumns(logger, columnsOnServer)
        val updatedColumnOnServer = transaction.db.dialect.tableColumns(*tables.toTypedArray())
        modifyAllColumns(logger, updatedColumnOnServer)
        logger.log("Migration completed!")
    }

    private fun MigrationContext.createMissingColumns(
        baseLogger: Logger,
        databaseColumns: Map<Table, List<ColumnMetadata>>
    ) {
        baseLogger.log("Creating missing columns...")
        val parentLogger = baseLogger["create"]

        tables.forEach { table ->
            parentLogger.log("Working on: ${normalizedTableName(table)}")

            val logger = parentLogger[normalizedTableName(table)]
            val columnsOnServer = databaseColumns.getValue(table)

            val newColumns = table.columns.filter { column ->
                columnsOnServer.none { metadata -> normalizedColumnName(column) == metadata.name }
            }

            if (newColumns.isEmpty()) {
                logger.log("No new columns were found")
                return@forEach
            }

            logger.log("New columns were found: ${newColumns.map { normalizedColumnName(it) } }, creating them...")

            val create = newColumns.flatMap { column -> column.createStatement() }
            execInBatch(logger, create)

            logger.log("New columns were created")
        }
    }

    private fun MigrationContext.modifyAllColumns(
        baseLogger: Logger,
        databaseColumns: Map<Table, List<ColumnMetadata>>
    ) {
        baseLogger.log("Modifying all columns...")
        val parentLogger = baseLogger["modify-all"]

        tables.forEach { table ->
            parentLogger.log("Working on ${normalizedTableName(table)}")
            val logger = parentLogger[normalizedTableName(table)]

            val columnsOnServer = databaseColumns.getValue(table)

            val modify = table.columns.map { column ->
                column to columnsOnServer.first { it.name == normalizedColumnName(column) }
            }.flatMap { (column, columnOnServer) ->
                // exposed doesn't support migration of auto inc
                if (column.columnType.isAutoInc) return@flatMap emptyList()

                column.modifyStatements(
                    ColumnDiff(
                        nullability = column.columnType.nullable != columnOnServer.nullable,
                        autoInc = false,
                        defaults = true,
                        caseSensitiveName = true
                    )
                )
            }
            execInBatch(logger, modify)
            logger.log("Columns were modified")
        }
    }

    private fun MigrationContext.execInBatch(
        baseLogger: Logger,
        statements: List<String>
    ) {
        statements.forEach(baseLogger.SQL::log)
        transaction.execInBatch(statements)
    }

    private fun MigrationContext.normalizedTableName(table: Table): String {
        return table.nameInDatabaseCase().removePrefix("${transaction.connection.schema}.")
    }

    private fun MigrationContext.normalizedColumnName(column: Column<*>): String {
        return column.nameInDatabaseCase()
    }

    override val displayName: String = "AutoMigration"

    private fun MigrationContext.cannotMigrate(): Nothing {
        transaction.rollback()
        throw CannotAutoMigrateException()
    }

    public class CannotAutoMigrateException : RuntimeException(
        /* message = */"Ambiguity occurred while migrating, cannot make auto migration for current state"
    )
}
