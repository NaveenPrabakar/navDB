package cachedb.benchmark;

import java.io.FileWriter;
import java.util.Map;

public class GetLatencyBenchmark extends CacheDBBenchmarkBase {

    public static void main(String[] args) throws Exception {
        int entries = 10_000;
        int iterations = 10_000;

        GetLatencyBenchmark bench = new GetLatencyBenchmark();
        bench.setup(3600);

        // preload
        for (int i = 0; i < entries; i++) {
            bench.cache.set(
                    "users",
                    Map.of("id", i),
                    Map.of("name", "User-" + i)
            );
        }

        // warmup
        for (int i = 0; i < 1_000; i++) {
            bench.cache.get("users", Map.of("id", i));
        }

        FileWriter out = new FileWriter("get_latency.csv");
        out.write("latency_ns\n");

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            bench.cache.get("users", Map.of("id", entries / 2));
            long latency = System.nanoTime() - start;
            out.write(latency + "\n");
        }

        out.close();
        System.out.println("GET latency benchmark complete.");
    }
}
