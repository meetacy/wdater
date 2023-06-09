package app.meetacy.database.updater

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table.Dual.default
import org.jetbrains.exposed.sql.Transaction

/**
 * Prototype class representing the context for executing migrations.
 * More methods can be added as needed.
 *
 * @param transaction The database transaction in which the migration is executed.
 */
public class MigrationContext(public val transaction: Transaction) {

    /**
     * Creates the column in the associated table.
     */
    public fun Column<*>.create() {
        transaction.execInBatch(createStatement())
    }

    /**
     * Creates the column in the associated table with a one-time initial value.
     *
     * @param oneTimeInitial The initial value for the column.
     */
    public fun <T> Column<T>.create(oneTimeInitial: T) {
        // First, create the table with the default value
        transaction.execInBatch(default(oneTimeInitial).createStatement())
        // Then, remove the default value
        transaction.execInBatch(modifyStatement())
    }

    /**
     * Modifies the column in the associated table.
     */
    public fun Column<*>.modify() {
        transaction.execInBatch(modifyStatement())
    }

    /**
     * Drops the column from the associated table.
     */
    public fun Column<*>.drop() {
        transaction.execInBatch(dropStatement())
    }
}
