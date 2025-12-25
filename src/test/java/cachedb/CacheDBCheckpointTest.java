package cachedb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CacheDBCheckpointTest {

    private static final Path WAL_PATH =
            Path.of("logs", "wal.log");

    private DataSource ds;

    @BeforeEach
    void setup() throws Exception {
        ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        Files.createDirectories(WAL_PATH.getParent());
        Files.deleteIfExists(WAL_PATH);
    }

    @Test
    void checkpointClearsWal() throws Exception {

        CacheDB db = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice")
        );

        // WAL should exist and be non-empty
        assertTrue(Files.size(WAL_PATH) > 0);

        // Force checkpoint
        db.checkpoint();

        // WAL should be empty
        assertEquals(0, Files.size(WAL_PATH));
    }

    @Test
    void recoveryDoesNotReplayAfterCheckpoint() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "users",
                Map.of("id", 2),
                Map.of("name", "Bob")
        );

        // Simulate successful flush + checkpoint
        db1.checkpoint();
        db1 = null; // crash

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // If WAL was cleared, recovery should NOT reinsert
        Map<String, Object> result =
                db2.get("users", Map.of("id", 2));

        assertNull(result);
    }

    @Test
    void recoveryStillWorksWithoutCheckpoint() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "users",
                Map.of("id", 3),
                Map.of("name", "Carol")
        );

        db1 = null; // crash before checkpoint

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        Map<String, Object> result =
                db2.get("users", Map.of("id", 3));

        assertNotNull(result);
        assertEquals("Carol", result.get("name"));
    }

    @Test
    void checkpointIsIdempotent() throws Exception {

        CacheDB db = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db.set(
                "users",
                Map.of("id", 4),
                Map.of("name", "Dave")
        );

        db.checkpoint();
        db.checkpoint(); // second call should not fail

        assertEquals(0, Files.size(WAL_PATH));
    }
}
