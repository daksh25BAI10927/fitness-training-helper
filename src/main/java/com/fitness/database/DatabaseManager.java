package com.fitness.database;

import com.fitness.analysis.SetRecord;
import com.fitness.workout.WorkoutSession;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Local SQLite-backed storage for workout history. Uses a single long-lived
 * {@link Connection} (the normal pattern for an embedded, single-user
 * desktop database) guarded by {@code synchronized} methods, since
 * sqlite-jdbc's connections are not safe for concurrent statement execution
 * and this app only ever has one thread touching the database at a time
 * (UI actions - save on stop, load/delete on the History screen).
 *
 * <p>The schema is created on first use if it does not already exist, so a
 * fresh install "just works" with no separate migration step. A session's
 * per-set breakdown is stored in a second table ({@code session_sets}) with
 * a foreign key back to {@code sessions}, deleted together with its parent.</p>
 */
public final class DatabaseManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    private final Connection connection;

    public DatabaseManager(String dbFilePath) throws DatabaseException {
        try {
            File file = new File(dbFilePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new DatabaseException("Could not create database directory: " + parent);
            }
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            initSchema();
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("SQLite JDBC driver not found on the classpath", e);
        } catch (SQLException e) {
            throw new DatabaseException("Could not open database at " + dbFilePath, e);
        }
    }

    private void initSchema() throws DatabaseException {
        String sessions = "CREATE TABLE IF NOT EXISTS sessions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "start_time_epoch_millis INTEGER NOT NULL, "
                + "total_reps INTEGER NOT NULL, "
                + "total_sets INTEGER NOT NULL, "
                + "workout_duration_ms INTEGER NOT NULL, "
                + "active_duration_ms INTEGER NOT NULL, "
                + "total_break_duration_ms INTEGER NOT NULL, "
                + "break_count INTEGER NOT NULL, "
                + "longest_rest_ms INTEGER NOT NULL, "
                + "average_rep_duration_ms INTEGER NOT NULL"
                + ")";
        String sets = "CREATE TABLE IF NOT EXISTS session_sets ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "session_id INTEGER NOT NULL, "
                + "set_number INTEGER NOT NULL, "
                + "rep_count INTEGER NOT NULL, "
                + "duration_ms INTEGER NOT NULL, "
                + "FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE"
                + ")";
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute(sessions);
            st.execute(sets);
        } catch (SQLException e) {
            throw new DatabaseException("Could not initialize database schema", e);
        }
    }

    /** Saves a finished session (and its per-set breakdown) and returns the generated session id. */
    public synchronized long saveSession(WorkoutSession session) throws DatabaseException {
        String insertSession = "INSERT INTO sessions "
                + "(start_time_epoch_millis, total_reps, total_sets, workout_duration_ms, active_duration_ms, "
                + "total_break_duration_ms, break_count, longest_rest_ms, average_rep_duration_ms) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertSet = "INSERT INTO session_sets (session_id, set_number, rep_count, duration_ms) VALUES (?, ?, ?, ?)";

        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new DatabaseException("Could not start database transaction", e);
        }

        try {
            long sessionId;
            try (PreparedStatement ps = connection.prepareStatement(insertSession)) {
                ps.setLong(1, session.getStartTimeEpochMillis());
                ps.setInt(2, session.getTotalReps());
                ps.setInt(3, session.getTotalSets());
                ps.setLong(4, session.getWorkoutDurationMs());
                ps.setLong(5, session.getActiveDurationMs());
                ps.setLong(6, session.getTotalBreakDurationMs());
                ps.setInt(7, session.getBreakCount());
                ps.setLong(8, session.getLongestRestMs());
                ps.setLong(9, session.getAverageRepDurationMs());
                ps.executeUpdate();
            }
            // sqlite-jdbc's PreparedStatement.getGeneratedKeys() is not implemented on
            // every driver build (verified during development), so the generated id is
            // fetched the standard SQLite way instead: a same-connection, same-transaction
            // query of the last inserted rowid.
            try (Statement st = connection.createStatement();
                 ResultSet keys = st.executeQuery("SELECT last_insert_rowid()")) {
                if (!keys.next()) {
                    throw new DatabaseException("Could not determine the generated session id");
                }
                sessionId = keys.getLong(1);
            }

            try (PreparedStatement ps = connection.prepareStatement(insertSet)) {
                for (SetRecord set : session.getSets()) {
                    ps.setLong(1, sessionId);
                    ps.setInt(2, set.getSetNumber());
                    ps.setInt(3, set.getRepCount());
                    ps.setLong(4, set.getDurationMs());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            connection.commit();
            session.setId(sessionId);
            return sessionId;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new DatabaseException("Could not save workout session", e);
        } finally {
            restoreAutoCommit(previousAutoCommit);
        }
    }

    /** Returns all saved sessions, most recent first. */
    public synchronized List<SessionRecord> listSessions() throws DatabaseException {
        String sql = "SELECT id, start_time_epoch_millis, total_reps, total_sets, workout_duration_ms, "
                + "active_duration_ms, total_break_duration_ms, break_count, longest_rest_ms, average_rep_duration_ms "
                + "FROM sessions ORDER BY start_time_epoch_millis DESC";
        List<SessionRecord> results = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new SessionRecord(
                        rs.getLong("id"),
                        rs.getLong("start_time_epoch_millis"),
                        rs.getInt("total_reps"),
                        rs.getInt("total_sets"),
                        rs.getLong("workout_duration_ms"),
                        rs.getLong("active_duration_ms"),
                        rs.getLong("total_break_duration_ms"),
                        rs.getInt("break_count"),
                        rs.getLong("longest_rest_ms"),
                        rs.getLong("average_rep_duration_ms")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load workout history", e);
        }
        return results;
    }

    /** Returns the per-set breakdown for one session, ordered by set number. */
    public synchronized List<SetRecord> getSetsForSession(long sessionId) throws DatabaseException {
        String sql = "SELECT set_number, rep_count, duration_ms FROM session_sets WHERE session_id = ? ORDER BY set_number";
        List<SetRecord> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new SetRecord(rs.getInt("set_number"), rs.getInt("rep_count"), rs.getLong("duration_ms")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load set details for session " + sessionId, e);
        }
        return results;
    }

    /** Deletes one session and its per-set rows. */
    public synchronized void deleteSession(long sessionId) throws DatabaseException {
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM session_sets WHERE session_id = ?")) {
                ps.setLong(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM sessions WHERE id = ?")) {
                ps.setLong(1, sessionId);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new DatabaseException("No session found with id " + sessionId);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete session " + sessionId, e);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Rollback failed after a failed save", e);
        }
    }

    private void restoreAutoCommit(boolean previous) {
        try {
            connection.setAutoCommit(previous);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Could not restore auto-commit mode", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error closing database connection", e);
        }
    }
}
