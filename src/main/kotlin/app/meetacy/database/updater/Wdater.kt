package app.meetacy.database.updater

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.math.log

/**
 * Create a new instance of [Wdater] with the provided configuration.
 *
 * @param block The configuration block to customize the [WdaterConfig.Builder].
 * @return The configured [Wdater] instance.
 */
public fun Wdater(block: WdaterConfig.Builder.() -> Unit): Wdater {
    val config = WdaterConfig().builder().apply(block).build()
    return Wdater(config)
}

/**
 * Wdater class that handles database migrations.
 *
 * @param config The configuration for the [Wdater] instance.
 */
public class Wdater(private val config: WdaterConfig = WdaterConfig()) {
    private val db = config.database
    private val storage = config.storage
    private val logger = config.logger["wdater"]

    /**
     * Update the database with the specified migrations.
     *
     * @param migrations The list of [Migration] instances representing the migrations to be executed.
     */
    public suspend fun update(vararg migrations: Migration) {
        update(migrations.toList())
    }

    /**
     * Update the database with the specified migrations.
     *
     * @param migrations The list of [Migration] instances representing the migrations to be executed.
     */
    public suspend fun update(migrations: List<Migration>) {
        logDependencies(migrations)
        logger.log("Process of database migration started")
        val fromVersion = storage.getSchemaVersion()
        // Skip migrations if there is no version
        if (fromVersion == null) {
            logger.log("No schema version was found, initializing database...")
            initializeDatabase(migrations)
        } else {
            logger.log("Detected schema version is: $fromVersion, running migrations...")
            val migratedVersion = migrate(fromVersion, migrations)
            logger.log("Saving the final version of schema using storage")
            storage.setSchemaVersion(migratedVersion)
        }
        logger.log("Completed!")
    }

    private suspend fun initializeDatabase(migrations: List<Migration>) {
        val logger = logger["db-init"]

        val maxMigrationsVersion = migrations.maxOfOrNull { it.toVersion }

        if (maxMigrationsVersion == null) {
            logger.log("There is no migrations, so I will use config.defaultSchemaVersion (${config.defaultSchemaVersion}) as the latest schema version")
        } else {
            logger.log("The latest schema version was found in migrations: $maxMigrationsVersion")
        }

        val maxVersion = maxMigrationsVersion ?: config.defaultSchemaVersion

        logger.log("Saving $maxVersion schema version using storage")
        storage.setSchemaVersion(maxVersion)

        if (config.initializer is DatabaseInitializer.Empty) {
            logger.log("No initializer provided")
        } else {
            logger.log("Running custom initializer...")
        }

        newSuspendedTransaction(Dispatchers.IO, db) {
            with(config.initializer) {
                MigrationContext(
                    transaction = this@newSuspendedTransaction,
                    logger = logger
                ).initialize()
            }
        }

        logger.log("Completed!")
    }

    /**
     * Perform migrations recursively from the specified [fromVersion] until the end.
     *
     * @param fromVersion The starting version of the migrations.
     * @param migrations The list of [Migration] instances representing the migrations to be executed.
     * @return The final version of the schema table after migration.
     */
    private tailrec suspend fun migrate(
        fromVersion: Int,
        migrations: List<Migration>
    ): Int {
        val logger = logger["migrating"]

        // If we did not find any migration to perform, assume we should stop there
        val migration = migrations.find { migration ->
            migration.fromVersion == fromVersion
        }

        if (migration == null) {
            logger.log("No migrations was found, therefore the ending point of migration is $fromVersion")
            return fromVersion
        }

        val migrationLogger = logger[stringifyMigration(migration)]
        migrationLogger.log("Migration found, running it...")

        newSuspendedTransaction(Dispatchers.IO, db) {
            with(migration) {
                MigrationContext(
                    transaction = this@newSuspendedTransaction,
                    logger = logger[stringifyMigration(migration)]
                ).migrate()
            }
        }

        migrationLogger.log("Migration completed, current schema version is ${migration.toVersion}")
        logger.log("Searching for the next migration...")

        return migrate(migration.toVersion, migrations)
    }

    private fun logDependencies(migrations: List<Migration>) {
        val logger = logger["dependencies"]

        if (db == null) {
            logger.log("• Database: ${TransactionManager.defaultDatabase?.name} (from global object)")
        } else {
            logger.log("• Database: ${db.name} (passed explicitly)")
        }

        if (storage is WdaterTable) {
            logger.log("• Migrations Storage: Default implementation is used for writing, table '${storage.tableName}'")
        } else {
            logger.log("• Migrations Storage: Custom implementation")
        }

        val initializerSpecified = config.initializer is DatabaseInitializer.Empty
        logger.log("• Initializer: ${if (initializerSpecified) "specified" else "unspecified"}")

        logger.log("• Migrations: ${stringifyMigrations(migrations)}")
    }

    private fun stringifyMigrations(migrations: List<Migration>): String =
        migrations.map(::stringifyMigration).toString()

    private fun stringifyMigration(migration: Migration): String =
        "${migration.displayName}{${migration.fromVersion} -> ${migration.toVersion}}"

    /**
     * Configure the [Wdater] instance using the provided configuration block.
     *
     * @param block The configuration block to customize the [WdaterConfig.Builder].
     * @return The configured [Wdater] instance.
     */
    public fun config(block: WdaterConfig.Builder.() -> Unit): Wdater {
        return Wdater(config.builder().apply(block).build())
    }
}
