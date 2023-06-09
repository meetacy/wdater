Wdater Kotlin Library
=====================

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT) 
[![GitHub issues](https://img.shields.io/github/issues/meetacy/wdater.svg)](https://github.com/meetacy/wdater/issues) 
[![GitHub stars](https://img.shields.io/github/stars/meetacy/wdater.svg)](https://github.com/meetacy/wdater/stargazers)

**Wdater** is a Kotlin library designed to simplify database migrations when used in conjunction with the Exposed library. It provides an intuitive and flexible way to manage database schema changes over time.

Pronounced as 'dub-dater' (database updater).

Features
--------

*   Easy creation and execution of database migrations.
*   Fluent API for defining migration steps.
*   Support for multiple migrations and version control.
*   Integration with Exposed library.

Installation
------------

> **Warning**
> 
> ⚠️ This is not yet available, library is only a snapshot ⚠️

Add the following dependency to your project's build.gradle file:

```kts
implementation('app.meetacy:wdater:$wdaterVersion')
```

Replace `$wdaterVersion` with the latest version from release.

Usage
-----

1.  Create an instance of `Wdater` using the `WdaterConfig.Builder`:

    ```kotlin
    // Configure the Wdater instance 
    val wdater = Wdater { 
        // Specify your Exposed database instance
        // If not specified, it will use global object
        database = myDatabase 
        // Specify the default schema version
        // Used if there is no migrations and `null` returned in storage
        defaultSchemaVersion = 0
        // Specify where to store info about migrations
        // If not specified, used a default variant with table named 'migrations' 
        storage = tableStorage(tableName = "migrations")
    }
    ```

2.  Define your database migrations by implementing the `Migration` interface:

    ```kotlin
    object `Migration 0-1` : Migration { 
        override val fromVersion = 0 
        
        override suspend fun MigrationContext.migrate() {
            // Create a new column
            UsersTable.USERNAME.create() 
            // Modify an existing column
            UsersTable.USERNAME.modify()
            // Drop an existing column
            UsersTable.USERNAME.drop()
        } 
    }
    ```
    
    <details>
    <summary>
        UsersTable.kt
    </summary>
        
    ```kotlin
    object UsersTable : Table() {
        val USER_ID = long("USER_ID").autoIncrement()
        val ACCESS_HASH = varchar("ACCESS_HASH", length = HASH_LENGTH)
        val NICKNAME = varchar("NICKNAME", length = NICKNAME_MAX_LIMIT)
        val USERNAME = varchar("USERNAME", length = USERNAME_MAX_LIMIT).nullable()
        val EMAIL = varchar("EMAIL", length = EMAIL_MAX_LIMIT).nullable()
        val EMAIL_VERIFIED = bool("EMAIL_VERIFIED").default(false)
        val AVATAR_ID = long("AVATAR_ID").nullable()
    }
    ```
    
    </details>

3.  Execute the migrations using the `update` function:

    ```kotlin
    wdater.update(`Migration 0-1`)
    ```
    
    You can also pass a list of migrations to update multiple steps at once.


Future Plans for Wdater
-----------------------

As part of our future plans for Wdater, we have the following extensions and modules in development:

1.  **Wkit**: This module will provide core types and interfaces for implementing database operations using JDBC or other methods. It will abstract away the underlying database dialect, making it more flexible and independent.

    *   **Wkit-JDBC**: This extension will provide an implementation of Wkit using JDBC.

    *   **Wkit-Exposed**: This extension will provide an implementation of Wkit using Exposed. It will utilize the global object in Exposed that stores the state of the connected database.

2.  **Wdater-Exposed**: This module will provide extensions on Exposed types, such as `Column` and `Table`, and will depend on Wdater and Wkit-Exposed. It will enhance the functionality of Exposed for database migrations.

3.  **Wdao**: This module, depending on Wkit, will offer an alternative to Exposed by providing a wrapper around the core Wkit functionality. It aims to address the limitations of Exposed and provide a more robust and efficient data access object (DAO) solution.


The motivation for this is to get rid of exposed as required dependency and support
other ORMs as well.

License
-------

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

Contributing
------------

Contributions are welcome! Here's how you can contribute:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Make the necessary changes and commit your code.
4.  Push your branch to your forked repository.
5.  Submit a pull request to the main repository.

Please ensure that your code follows the existing coding style and includes appropriate tests.

If you find any issues or have suggestions for improvement, please [open an issue](https://github.com/meetacy/wdater/issues) on GitHub.

We appreciate your contributions!
