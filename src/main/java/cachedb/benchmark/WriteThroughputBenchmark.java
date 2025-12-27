package cachedb.benchmark;

import java.util.Map;

public class WriteThroughputBenchmark extends CacheDBBenchmarkBase {

    public static void main(String[] args) throws Exception {
        int writes = 10_000;

        WriteThroughputBenchmark bench = new WriteThroughputBenchmark();
        bench.setup(300);

        long start = System.currentTimeMillis();

        for (int i = 0; i < writes; i++) {
            bench.cache.set(
                    "users",
                    Map.of("id", i),
                    Map.of("name", "User-" + i)
            );
        }

        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        double ops = writes / seconds;

        System.out.println("Writes: " + writes);
        System.out.println("Time (s): " + seconds);
        System.out.println("Throughput (ops/sec): " + ops);
    }
}
