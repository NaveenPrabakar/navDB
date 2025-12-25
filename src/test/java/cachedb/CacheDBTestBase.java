package cachedb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.IOException;

public abstract class CacheDBTestBase {

    protected CacheDB cache;

    @BeforeEach
    void setUp() throws IOException {
        DataSource ds = new SimpleDataSource(
                "jdbc:mysql://localhost:3306/cachedb",
                "root",
                "defg5678@"
        );

        cache = CacheDB.builder()
                .dataSource(ds)
                .ttlSeconds(2)
                .build();
    }
}
