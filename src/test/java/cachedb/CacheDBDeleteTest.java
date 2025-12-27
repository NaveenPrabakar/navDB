package cachedb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CacheDBDeleteTest {

    private DataSource ds;

    @BeforeEach
    void setup() throws Exception {
        ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        // Clean WAL before every test
        Path wal = Path.of("logs", "wal.log");
        Files.createDirectories(wal.getParent());
        Files.deleteIfExists(wal);
    }

    @Test
    void basicDelete() throws Exception {
        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // Set data
        cache.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice", "email", "alice@test.com")
        );

        // Verify it exists
        Map<String, Object> result = cache.get("users", Map.of("id", 1));
        assertNotNull(result);
        assertEquals("Alice", result.get("name"));

        // Delete it
        cache.delete("users", Map.of("id", 1));

        // Verify it's gone from cache
        result = cache.get("users", Map.of("id", 1));
        assertNull(result);
    }

    @Test
    void deleteNonExistentEntry() throws Exception {
        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // Try to delete non-existent entry
        // Should not throw exception, just return silently
        assertDoesNotThrow(() -> {
            cache.delete("users", Map.of("id", 999));
        });

        // Verify it's still null
        assertNull(cache.get("users", Map.of("id", 999)));
    }

    @Test
    void deleteIsFlushedToDatabase() throws Exception {
        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(2)
                .build();

        // Set data
        cache.set(
                "users",
                Map.of("id", 2),
                Map.of("name", "Bob")
        );

        // Delete it
        cache.delete("users", Map.of("id", 2));

        // Wait for TTL to expire and flush
        Thread.sleep(3000);

        // Entry should be removed from cache
        assertNull(cache.get("users", Map.of("id", 2)));
    }

    @Test
    void deleteWithCompositeKey() throws Exception {
        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // Set data with composite key
        cache.set(
                "order_items",
                Map.of("order_id", 100, "item_id", 3),
                Map.of("qty", 2, "price", 19.99)
        );

        // Verify it exists
        Map<String, Object> result = cache.get(
                "order_items",
                Map.of("order_id", 100, "item_id", 3)
        );
        assertNotNull(result);
        assertEquals(2, result.get("qty"));

        // Delete it
        cache.delete(
                "order_items",
                Map.of("order_id", 100, "item_id", 3)
        );

        // Verify it's gone
        result = cache.get(
                "order_items",
                Map.of("order_id", 100, "item_id", 3)
        );
        assertNull(result);
    }

    @Test
    void deleteRecoveryFromWAL() throws Exception {
        // ---------- First "process" ----------
        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "users",
                Map.of("id", 3),
                Map.of("name", "Charlie")
        );

        // Delete it
        db1.delete("users", Map.of("id", 3));

        // Simulate crash (no shutdown)
        db1 = null;

        // ---------- Second "process" ----------
        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // After recovery, entry should be deleted (null)
        Map<String, Object> result = db2.get("users", Map.of("id", 3));
        assertNull(result);
    }

    @Test
    void deleteAfterSetRecovery() throws Exception {
        // ---------- First "process" ----------
        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "users",
                Map.of("id", 4),
                Map.of("name", "David")
        );

        db1.delete("users", Map.of("id", 4));

        // Set again after delete
        db1.set(
                "users",
                Map.of("id", 4),
                Map.of("name", "David-v2")
        );

        // Simulate crash
        db1 = null;

        // ---------- Second "process" ----------
        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // Should recover the latest write (after delete)
        Map<String, Object> result = db2.get("users", Map.of("id", 4));
        assertNotNull(result);
        assertEquals("David-v2", result.get("name"));
    }

    @Test
    void deleteMultipleEntries() throws Exception {
        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // Set multiple entries
        cache.set("users", Map.of("id", 5), Map.of("name", "Eve"));
        cache.set("users", Map.of("id", 6), Map.of("name", "Frank"));
        cache.set("users", Map.of("id", 7), Map.of("name", "Grace"));

        // Verify they exist
        assertNotNull(cache.get("users", Map.of("id", 5)));
        assertNotNull(cache.get("users", Map.of("id", 6)));
        assertNotNull(cache.get("users", Map.of("id", 7)));

        // Delete one
        cache.delete("users", Map.of("id", 6));

        // Verify only that one is deleted
        assertNotNull(cache.get("users", Map.of("id", 5)));
        assertNull(cache.get("users", Map.of("id", 6)));
        assertNotNull(cache.get("users", Map.of("id", 7)));
    }

    @Test
    void deleteThenReinsert() throws Exception {
        CacheDB cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        // Set data
        cache.set("users", Map.of("id", 8), Map.of("name", "Henry"));

        // Delete it
        cache.delete("users", Map.of("id", 8));
        assertNull(cache.get("users", Map.of("id", 8)));

        // Reinsert
        cache.set("users", Map.of("id", 8), Map.of("name", "Henry-v2"));

        // Should be available again
        Map<String, Object> result = cache.get("users", Map.of("id", 8));
        assertNotNull(result);
        assertEquals("Henry-v2", result.get("name"));
    }
}

