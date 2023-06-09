package app.meetacy.database.updater

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

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
        val fromVersion = storage.getSchemaVersion()

        // Skip migrations if there is no version
        if (fromVersion == null) {
            initializeDatabase(migrations)
        } else {
            val migratedVersion = migrate(fromVersion, migrations)
            storage.setSchemaVersion(migratedVersion)
        }
    }

    private suspend fun initializeDatabase(migrations: List<Migration>) {
        val maxVersion = migrations.maxOfOrNull { it.toVersion } ?: config.defaultSchemaVersion
        storage.setSchemaVersion(maxVersion)
        newSuspendedTransaction(Dispatchers.IO, db) {
            with(config.initializer) {
                MigrationContext(transaction = this@newSuspendedTransaction).initialize()
            }
        }
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
        // If we did not find any migration to perform, assume we should stop there
        val migration = migrations.find { migration ->
            migration.fromVersion == fromVersion
        } ?: return fromVersion

        newSuspendedTransaction(Dispatchers.IO, db) {
            with(migration) {
                MigrationContext(transaction = this@newSuspendedTransaction).migrate()
            }
        }

        return migrate(migration.toVersion, migrations)
    }

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
