package cachedb;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class CacheDB {

    private static final Path WAL_PATH =
            Path.of("logs", "wal.log");

    private final CacheStore store;
    private final ExpirationManager expirationManager;
    private final WALWriter wal;

    private CacheDB(CacheStore store,
                    ExpirationManager expirationManager) throws IOException {

        this.store = store;
        this.expirationManager = expirationManager;

        Files.createDirectories(WAL_PATH.getParent());
        this.wal = new WALWriter(WAL_PATH);

        recover();
    }

    public synchronized void checkpoint() {
        try {
            wal.sync();
            wal.truncate();
        } catch (Exception e) {
            // swallow
        }
    }



    private void recover() throws IOException {
        if (!Files.exists(WAL_PATH)) return;

        WALReader reader = new WALReader(WAL_PATH);

        for (LogRecord r : reader) {
            if (r.type() != LogType.PUT) continue;

            String key = new String(r.key());
            String value = new String(r.value());

            // key format: table|{pk}
            String[] parts = key.split("\\|", 2);
            String table = parts[0];
            Map<String, Object> pk = SimpleCodec.parseMap(parts[1]);
            Map<String, Object> cols = SimpleCodec.parseMap(value);

            store.upsert(table, pk, cols);
        }
    }

    public void set(String table,
                    Map<String, Object> primaryKey,
                    Map<String, Object> columns) {

        Objects.requireNonNull(table);
        Objects.requireNonNull(primaryKey);
        Objects.requireNonNull(columns);

        byte[] walKey =
                (table + "|" + primaryKey.toString()).getBytes();
        byte[] walValue =
                columns.toString().getBytes();

        try {
            wal.append(LogRecord.put(walKey, walValue));
        } catch (IOException e) {
            throw new RuntimeException("WAL write failed", e);
        }

        store.upsert(table, primaryKey, columns);
    }

    public Map<String, Object> get(String table,
                                   Map<String, Object> primaryKey) {
        return store.get(table, primaryKey);
    }

    /* ------------ BUILDER ------------ */

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private DataSource dataSource;
        private long ttlMillis = 2000;

        public Builder dataSource(DataSource ds) {
            this.dataSource = ds;
            return this;
        }

        public Builder ttlSeconds(long seconds) {
            this.ttlMillis = seconds * 1000;
            return this;
        }

        public CacheDB build() throws IOException {
            Objects.requireNonNull(dataSource);

            SchemaRegistry schemaRegistry =
                    new SchemaRegistry(dataSource);

            CacheStore store = new CacheStore(ttlMillis);

            FlushManager flushManager =
                    new FlushManager(dataSource, schemaRegistry);

            ExpirationManager expirationManager =
                    new ExpirationManager(store, flushManager);

            new Thread(flushManager, "flush-thread").start();
            new Thread(expirationManager, "expiration-thread").start();

            return new CacheDB(store, expirationManager);
        }


    }
}
