package app.meetacy.database.updater

import org.jetbrains.exposed.sql.Database

/**
 * Configuration class for Wdater.
 *
 * @property database The Exposed database instance.
 * @property storage The storage implementation for managing migrations.
 * @property defaultSchemaVersion The default schema version.
 * @property initializer The initializer for performing database initialization.
 */
public class WdaterConfig(
    public val database: Database? = null,
    public val storage: WdaterStorage = WdaterTable(database = database),
    public val defaultSchemaVersion: Int = 0,
    public val initializer: DatabaseInitializer = DatabaseInitializer.Empty
) {
    /**
     * Creates a new [Builder] instance to configure the [WdaterConfig].
     */
    public fun builder(): Builder = Builder(database, storage, defaultSchemaVersion)

    /**
     * Builder class for configuring the [WdaterConfig].
     */
    public class Builder(
        public var database: Database? = null,
        public var storage: WdaterStorage = WdaterTable(database = database),
        public var defaultSchemaVersion: Int = 0,
        public var initializer: DatabaseInitializer = DatabaseInitializer.Empty
    ) {
        /**
         * Creates the table storage configuration for managing migrations.
         *
         * @param name The name of the migrations table.
         * @param database The Exposed database instance.
         * @return The [WdaterStorage] instance.
         */
        public fun tableStorage(
            name: String = "migrations",
            database: Database? = this.database
        ): WdaterStorage {
            return WdaterTable(name, database)
        }

        /**
         * Sets the initializer for performing additional database initialization.
         *
         * @param block The block of code to execute during initialization.
         */
        public inline fun initializer(crossinline block: suspend () -> Unit) {
            this.initializer = object : DatabaseInitializer {
                override suspend fun MigrationContext.initialize() {
                    block()
                }
            }
        }

        /**
         * Builds the [WdaterConfig] instance with the configured values.
         *
         * @return The [WdaterConfig] instance.
         */
        public fun build(): WdaterConfig = WdaterConfig(
            database = database,
            storage = storage,
            defaultSchemaVersion = defaultSchemaVersion,
            initializer = initializer
        )
    }
}
