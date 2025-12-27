package cachedb.benchmark;

import java.io.IOException;
import java.util.Map;

public class CrashRecoveryWriter extends CacheDBBenchmarkBase {

    public static void main(String[] args) throws IOException {
        CrashRecoveryWriter bench = new CrashRecoveryWriter();
        bench.setup(30);

        bench.cache.set(
                "users",
                Map.of("id", 42),
                Map.of("name", "Alice")
        );

        System.out.println("Write completed. Simulating crash.");
        System.exit(1); // simulate crash
    }
}
