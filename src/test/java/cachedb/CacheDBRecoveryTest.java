package cachedb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CacheDBRecoveryTest {

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
    void recoversAfterRestart() throws Exception {

        // ---------- First "process" ----------
        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "users",
                Map.of("id", 1),
                Map.of("name", "Alice")
        );

        // Simulate crash (no shutdown)
        db1 = null;

        // ---------- Second "process" ----------
        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        Map<String, Object> result =
                db2.get("users", Map.of("id", 1));

        assertNotNull(result);
        assertEquals("Alice", result.get("name"));
    }

    @Test
    void recoversLatestWrite() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set("users", Map.of("id", 1),
                Map.of("name", "Alice"));

        db1.set("users", Map.of("id", 1),
                Map.of("name", "Alice-v2"));

        db1 = null; // crash

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        Map<String, Object> result =
                db2.get("users", Map.of("id", 1));

        assertEquals("Alice-v2", result.get("name"));
    }

    @Test
    void recoversMultipleTables() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set("users", Map.of("id", 1),
                Map.of("name", "Alice"));

        db1.set("orders", Map.of("order_id", 100),
                Map.of("status", "PAID"));

        db1 = null;

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        assertEquals(
                "Alice",
                db2.get("users", Map.of("id", 1)).get("name")
        );

        assertEquals(
                "PAID",
                db2.get("orders", Map.of("order_id", 100)).get("status")
        );
    }

    @Test
    void recoveryWorksWithCompositePrimaryKey() throws Exception {

        CacheDB db1 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        db1.set(
                "order_items",
                Map.of("order_id", 100, "item_id", 3),
                Map.of("qty", 2)
        );

        db1 = null;

        CacheDB db2 = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(10)
                .build();

        Map<String, Object> result =
                db2.get(
                        "order_items",
                        Map.of("order_id", 100, "item_id", 3)
                );

        assertEquals("2", result.get("qty"));
    }
}
