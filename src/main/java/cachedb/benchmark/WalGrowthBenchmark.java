package cachedb.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class WalGrowthBenchmark extends CacheDBBenchmarkBase {

    public static void main(String[] args) throws Exception {
        int writes = 10_000;

        WalGrowthBenchmark bench = new WalGrowthBenchmark();
        bench.setup(60);

        for (int i = 0; i < writes; i++) {
            bench.cache.set(
                    "users",
                    Map.of("id", i),
                    Map.of("name", "User-" + i)
            );
        }

        long size = Files.size(Path.of("logs/wal.log"));


        System.out.println("Writes: " + writes);
        System.out.println("WAL size (bytes): " + size);
    }
}
