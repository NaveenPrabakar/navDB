package bench;
import java.sql.*;

public class MySQLBench {

    public static void run() throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/cachedb",
            "root",
            "defg5678@"
        );

        PreparedStatement ps = conn.prepareStatement(
            "SELECT v FROM kv WHERE k = ?"
        );

        LatencyStats stats = new LatencyStats(100_000);

        // Warmup
        for (int i = 0; i < 10_000; i++) {
            ps.setString(1, "key" + i);
            ps.executeQuery();
        }

        for (int i = 0; i < 100_000; i++) {
            String key = "key" + (i % 100_000);
            long start = System.nanoTime();
            ps.setString(1, key);
            ps.executeQuery();
            stats.record(System.nanoTime() - start);
        }

        stats.report("MySQL");
        conn.close();
    }
}
