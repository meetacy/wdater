package app.meetacy.database.updater

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Interface defining the storage methods for the Wdater library.
 */
public interface WdaterStorage {

    /**
     * Retrieves the current schema version from the storage.
     *
     * @return The current schema version, or null if it is not set.
     */
    public suspend fun getSchemaVersion(): Int?

    /**
     * Sets the schema version in the storage.
     *
     * @param version The schema version to set.
     */
    public suspend fun setSchemaVersion(version: Int)
}

/**
 * Table implementation of the [WdaterStorage] interface for storing schema version information.
 *
 * @param name The name of the table.
 * @param database The Exposed [Database] instance to use. If null, the default database will be used.
 */
@Suppress("PropertyName")
public class WdaterTable(
    name: String = "migrations",
    private val database: Database? = null
) : Table(name), WdaterStorage {

    /**
     * Column representing the schema version.
     */
    public val SCHEMA_VERSION: Column<Int?> = integer("version").nullable().default(null)

    init {
        transaction(database) {
            SchemaUtils.create(this@WdaterTable)
        }
    }

    /**
     * Retrieves the current schema version from the table.
     *
     * @return The current schema version, or null if it is not set.
     */
    override suspend fun getSchemaVersion(): Int? {
        return newSuspendedTransaction(Dispatchers.IO, database) {
            selectAll().firstOrNull()?.get(SCHEMA_VERSION)
        }
    }

    /**
     * Sets the schema version in the table.
     *
     * @param version The schema version to set.
     */
    override suspend fun setSchemaVersion(version: Int) {
        newSuspendedTransaction(Dispatchers.IO, database) {
            deleteAll()
            insert { it[SCHEMA_VERSION] = version }
        }
    }
}
