package cachedb.benchmark;

import java.io.FileWriter;
import java.util.Map;

public class SetLatencyBenchmark extends CacheDBBenchmarkBase {

    public static void main(String[] args) throws Exception {
        int iterations = 10_000;

        SetLatencyBenchmark bench = new SetLatencyBenchmark();
        bench.setup(3600);

        FileWriter out = new FileWriter("set_latency.csv");
        out.write("latency_ns\n");

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            bench.cache.set(
                    "users",
                    Map.of("id", i),
                    Map.of("name", "User-" + i)
            );
            long latency = System.nanoTime() - start;
            out.write(latency + "\n");
        }

        out.close();
        System.out.println("SET latency benchmark complete.");
    }
}
