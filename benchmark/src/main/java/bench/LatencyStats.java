package bench;
import java.util.*;

public class LatencyStats {
    private final long[] samples;
    private int idx = 0;

    public LatencyStats(int size) {
        samples = new long[size];
    }

    public void record(long nanos) {
        samples[idx++] = nanos;
    }

    public void report(String name) {
        Arrays.sort(samples);
        System.out.printf(
            "%s → p50=%dµs p95=%dµs p99=%dµs avg=%dµs%n",
            name,
            samples[samples.length / 2] / 1_000,
            samples[(int)(samples.length * 0.95)] / 1_000,
            samples[(int)(samples.length * 0.99)] / 1_000,
            Arrays.stream(samples).sum() / samples.length / 1_000
        );
    }
}
