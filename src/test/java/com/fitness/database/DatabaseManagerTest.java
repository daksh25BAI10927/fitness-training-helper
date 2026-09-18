package com.fitness.database;

import com.fitness.analysis.SetRecord;
import com.fitness.workout.WorkoutSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DatabaseManager}.
 *
 * <p>Uses a temporary directory for the SQLite database file so tests
 * don't pollute the user's real workout history.</p>
 */
public class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() throws DatabaseException {
        String dbPath = tempDir.resolve("test_workout.db").toString();
        dbManager = new DatabaseManager(dbPath);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testEmptyDatabase() throws DatabaseException {
        List<SessionRecord> sessions = dbManager.listSessions();
        assertTrue(sessions.isEmpty(), "Empty database should return empty list");
    }

    @Test
    void testSaveAndRetrieveSession() throws DatabaseException {
        WorkoutSession session = createTestSession(100, 10, 3);
        long id = dbManager.saveSession(session);
        assertTrue(id > 0, "Should return valid session ID");

        List<SessionRecord> sessions = dbManager.listSessions();
        assertEquals(1, sessions.size());

        SessionRecord record = sessions.get(0);
        assertEquals(id, record.getId());
        assertEquals(10, record.getTotalReps());
        assertEquals(3, record.getTotalSets());
    }

    @Test
    void testSessionsOrderedNewestFirst() throws DatabaseException {
        WorkoutSession older = createTestSession(1000, 5, 1);
        WorkoutSession newer = createTestSession(2000, 10, 2);

        dbManager.saveSession(older);
        dbManager.saveSession(newer);

        List<SessionRecord> sessions = dbManager.listSessions();
        assertEquals(2, sessions.size());
        assertEquals(2000, sessions.get(0).getStartTimeEpochMillis());
        assertEquals(1000, sessions.get(1).getStartTimeEpochMillis());
    }

    @Test
    void testDeleteSession() throws DatabaseException {
        WorkoutSession session = createTestSession(100, 5, 1);
        long id = dbManager.saveSession(session);

        dbManager.deleteSession(id);

        List<SessionRecord> sessions = dbManager.listSessions();
        assertTrue(sessions.isEmpty(), "Session should be deleted");
    }

    @Test
    void testDeleteNonExistentSessionThrows() {
        assertThrows(DatabaseException.class, () -> dbManager.deleteSession(99999L),
                "Deleting non-existent session should throw");
    }

    @Test
    void testSaveAndRetrieveSets() throws DatabaseException {
        List<SetRecord> sets = new ArrayList<>();
        sets.add(new SetRecord(1, 10, 30000));
        sets.add(new SetRecord(2, 12, 35000));

        WorkoutSession session = new WorkoutSession(
                null, System.currentTimeMillis(), 22, 2,
                120000, 65000, 55000, 1, 30000, sets);

        long id = dbManager.saveSession(session);

        List<SetRecord> retrieved = dbManager.getSetsForSession(id);
        assertEquals(2, retrieved.size());
        assertEquals(1, retrieved.get(0).getSetNumber());
        assertEquals(10, retrieved.get(0).getRepCount());
        assertEquals(30000, retrieved.get(0).getDurationMs());
        assertEquals(2, retrieved.get(1).getSetNumber());
        assertEquals(12, retrieved.get(1).getRepCount());
    }

    @Test
    void testDeleteCascadesSets() throws DatabaseException {
        List<SetRecord> sets = List.of(new SetRecord(1, 5, 15000));
        WorkoutSession session = new WorkoutSession(
                null, System.currentTimeMillis(), 5, 1,
                60000, 30000, 30000, 1, 15000, sets);

        long id = dbManager.saveSession(session);
        dbManager.deleteSession(id);

        List<SetRecord> retrieved = dbManager.getSetsForSession(id);
        assertTrue(retrieved.isEmpty(), "Sets should be deleted when session is deleted");
    }

    private WorkoutSession createTestSession(long startTime, int reps, int sets) {
        return new WorkoutSession(
                null,
                startTime,
                reps,
                sets,
                60000L,  // workoutDurationMs
                45000L,  // activeDurationMs
                15000L,  // totalBreakDurationMs
                1,       // breakCount
                10000L,  // longestRestMs
                Collections.emptyList() // sets
        );
    }
}
