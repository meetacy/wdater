package app.meetacy.database.updater

/**
 * Interface for initializing a database
 */
public interface DatabaseInitializer {

    /**
     * Initializes the database
     */
    public suspend fun MigrationContext.initialize()

    /**
     * Empty implementation of [DatabaseInitializer] that does nothing
     */
    public object Empty : DatabaseInitializer {
        /**
         * No-op implementation of [initialize]
         */
        override suspend fun MigrationContext.initialize() {}
    }
}
