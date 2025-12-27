package cachedb.benchmark;

import java.util.Map;

public class CrashRecoveryReader extends CacheDBBenchmarkBase {

    public static void main(String[] args) throws Exception {
        CrashRecoveryReader bench = new CrashRecoveryReader();

        // IMPORTANT: initialize CacheDB
        bench.setup(3600); // TTL irrelevant for recovery

        Object result = bench.cache.get(
                "users",
                Map.of("id", 1)
        );

        System.out.println("Recovered value: " + result);
    }
}
