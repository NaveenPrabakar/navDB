package bench;
public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        CacheDBBench.run();
        RedisBench.run();
        MySQLBench.run();
    }
}
