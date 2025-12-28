package bench;

import redis.clients.jedis.Jedis;

public class RedisBench {

    public static void run() {
        Jedis jedis = new Jedis("localhost", 6379);

        // Preload
        for (int i = 0; i < 100_000; i++) {
            jedis.set("key" + i, "value" + i);
        }

        LatencyStats stats = new LatencyStats(100_000);

        // Warmup
        for (int i = 0; i < 10_000; i++) {
            jedis.get("key" + i);
        }

        // Measure
        for (int i = 0; i < 100_000; i++) {
            String key = "key" + (i % 100_000);
            long start = System.nanoTime();
            jedis.get(key);
            stats.record(System.nanoTime() - start);
        }

        stats.report("Redis (Jedis)");

        jedis.close();
    }
}
