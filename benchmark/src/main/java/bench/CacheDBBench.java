package bench;
import cachedb.CacheDB;
import cachedb.SimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;

public class CacheDBBench {

    public static void run() throws Exception {
        DataSource ds = new SimpleDataSource(
            "jdbc:mysql://localhost:3306/cachedb",
            "root",
            "defg5678@"
        );

        CacheDB cache = CacheDB.builder()
            .dataSource(ds)
            .ttlSeconds(100000)        // disable TTL
            .dashboard(false)    // disable dashboard
            .build();

        // Preload
        for (int i = 0; i < 100_000; i++) {
            cache.set(
                "kv",
                Map.of("k", "key" + i),
                Map.of("v", "value" + i)
            );
        }

        LatencyStats stats = new LatencyStats(100_000);

        // Warmup
        for (int i = 0; i < 10_000; i++) {
            cache.get("kv", Map.of("k", "key" + i));
        }

        // Measure
        for (int i = 0; i < 100_000; i++) {
            String key = "key" + (i % 100_000);
            long start = System.nanoTime();
            cache.get("kv", Map.of("k", key));
            stats.record(System.nanoTime() - start);
        }

        stats.report("CacheDB");
    }
}
