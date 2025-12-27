package cachedb.benchmark;

import cachedb.CacheDB;
import cachedb.SimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;

public abstract class CacheDBBenchmarkBase {

    protected CacheDB cache;

    protected void setup(int ttlSeconds) throws IOException {
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(ttlSeconds)
                .build();
    }
}
