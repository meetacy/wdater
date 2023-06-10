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
     * The string used as name when printing this migration
     */
    public val displayName: String get() = "Migration"

    /**
     * Function that defines the migration logic
     */
    public suspend fun MigrationContext.migrate()
}
