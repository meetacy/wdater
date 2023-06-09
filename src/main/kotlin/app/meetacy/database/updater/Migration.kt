package app.meetacy.database.updater

/**
 * Interface representing a database migration
 */
public interface Migration {

    /**
     * The version number representing the starting point of the migration
     */
    public val fromVersion: Int

    /**
     * The version number representing the ending point of the migration
     */
    public val toVersion: Int get() = fromVersion + 1

    /**
     * Function that defines the migration logic
     */
    public suspend fun MigrationContext.migrate()
}
