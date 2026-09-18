package com.fitness.database;

/** Thrown when the local SQLite database can't be opened, migrated, read, or written. */
public class DatabaseException extends Exception {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
