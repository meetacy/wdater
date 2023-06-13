package app.meetacy.database.updater.auto

import app.meetacy.database.updater.Migration
import app.meetacy.database.updater.MigrationContext
import app.meetacy.database.updater.log.Logger
import app.meetacy.database.updater.log.SQL
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.ColumnMetadata
import kotlin.math.log

public fun AutoMigration(
    vararg tables: Table,
    fromVersion: Int,
    toVersion: Int = fromVersion + 1
): AutoMigration = AutoMigration(tables.toList(), fromVersion, toVersion)

/**
 * Auto migrations do not support renaming
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
        val tablesOnServer = transaction.db.dialect.allTablesNames()
        removeObsoleteTables(logger,  tablesOnServer)
        createMissingTables(logger, tablesOnServer)
    }

    /**
     * @return whether the migration should be continued or not
     */
    private fun MigrationContext.removeObsoleteTables(
        baseLogger: Logger,
        tablesOnServer: List<String>
    ) {
        baseLogger.log("Removing obsolete tables...")
        val logger = baseLogger["remove"]

        val obsoleteTables = tablesOnServer
            .filter { name ->
                tables.none { table -> table.tableName == name }
            }

        if (obsoleteTables.isEmpty()) {
            logger.log("No obsolete tables were found")
            return
        }

        val hasNewTables = tables.any { table ->
            tablesOnServer.none { tableName -> table.tableName == tableName }
        }

        if (hasNewTables) {
            logger.log("There are both new and obsolete tables, so we can't delete them in order " +
                    "not to lose data in case the table was renamed. Aborting migration...")
            cannotMigrate()
        }

        logger.log("Found obsolete tables: $obsoleteTables, dropping them...")

        val drop = obsoleteTables.flatMap { tableName ->
            Table(tableName).dropStatement()
        }
        execInBatch(logger, drop)

        logger.log("Obsolete tables were dropped")
    }

    private fun MigrationContext.createMissingTables(
        baseLogger: Logger,
        tablesOnServer: List<String>
    ) {
        baseLogger.log("Creating missing tables...")
        val logger = baseLogger["create"]

        val newTables = tables.filter { table ->
            tablesOnServer.none { name -> table.tableName == name }
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
        tables.forEach { table ->
            logger.log("Migrating ${table.tableName}...")
            val tableLogger = logger[table.tableName]
            removeObsoleteColumns(tableLogger, table, columnsOnServer.getValue(table))
            createMissingColumns(tableLogger, table, columnsOnServer.getValue(table))
            modifyAllColumns(tableLogger, table)
        }
        logger.log("Migration completed!")
    }

    private fun MigrationContext.removeObsoleteColumns(
        baseLogger: Logger,
        table: Table,
        columnsOnServer: List<ColumnMetadata>
    ) {
        baseLogger.log("Removing obsolete columns...")
        val logger = baseLogger["remove"]

        val obsoleteColumns = columnsOnServer.filter { metadata ->
            table.columns.none { column ->
                column.name == metadata.name
            }
        }

        if (obsoleteColumns.isEmpty()) {
            logger.log("No obsolete columns found")
            return
        }

        val hasNewColumns = table.columns.any { column ->
            columnsOnServer.none { metadata ->
                metadata.name == column.name
            }
        }

        if (hasNewColumns) {
            logger.log("There are both new and obsolete columns, so we can't delete them in order " +
                    "not to lose data in case the table was renamed. Aborting migration...")
            cannotMigrate()
        }

        logger.log("Found obsolete columns: ${obsoleteColumns.map(ColumnMetadata::name)}, dropping them...")

        val drop = obsoleteColumns.flatMap { metadata ->
            table.dropColumnStatement(metadata.name)
        }
        execInBatch(logger, drop)

        logger.log("Obsolete columns were dropped")
    }

    private fun MigrationContext.createMissingColumns(
        baseLogger: Logger,
        table: Table,
        columnsOnServer: List<ColumnMetadata>
    ) {
        baseLogger.log("Creating missing columns...")
        val logger = baseLogger["create"]

        val newColumns = table.columns.filter { column ->
            columnsOnServer.none { metadata -> column.name == metadata.name }
        }

        if (newColumns.isEmpty()) {
            logger.log("No new columns were found")
            return
        }

        logger.log("New columns were found: ${newColumns.map(Column<*>::name)}, creating them...")

        val create = newColumns.flatMap { column -> column.createStatement() }
        execInBatch(logger, create)

        logger.log("New columns were created")
    }

    private fun MigrationContext.modifyAllColumns(
        baseLogger: Logger,
        table: Table
    ) {
        baseLogger.log("Modifying all columns...")
        val logger = baseLogger["modify-all"]
        val modify = table.columns.flatMap { it.modifyStatement() }
        execInBatch(logger, modify)
        logger.log("Columns were modified")
    }

    private fun MigrationContext.execInBatch(
        baseLogger: Logger,
        statements: List<String>
    ) {
        statements.forEach(baseLogger.SQL::log)
        transaction.execInBatch(statements)
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
