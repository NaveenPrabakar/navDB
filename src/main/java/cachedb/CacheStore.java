package cachedb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheStore {

    private final long ttlMillis;

    // table → keyHash → entry
    private final Map<String, Map<String, CacheEntry>> store =
            new ConcurrentHashMap<>();

    public CacheStore(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public void upsert(String table,
                       Map<String, Object> pk,
                       Map<String, Object> columns) {

        String keyHash = hash(pk);
        long now = System.currentTimeMillis();

        store.computeIfAbsent(table, t -> new ConcurrentHashMap<>())
                .compute(keyHash, (k, existing) -> {
                    if (existing == null) {
                        return new CacheEntry(pk, columns, now + ttlMillis);
                    }
                    existing.columns = columns;
                    existing.version++;
                    existing.dirty = true;
                    existing.expiresAt = now + ttlMillis;
                    return existing;
                });
    }

    public Map<String, Object> get(String table, Map<String, Object> pk) {
        Map<String, CacheEntry> tableMap = store.get(table);
        if (tableMap == null) return null;

        CacheEntry entry = tableMap.get(hash(pk));
        if (entry == null) return null;

        if (System.currentTimeMillis() > entry.expiresAt) {
            return null;
        }
        return entry.columns;
    }

    public boolean delete(String table, Map<String, Object> pk) {
        Map<String, CacheEntry> tableMap = store.get(table);
        if (tableMap == null) return false;

        String keyHash = hash(pk);
        CacheEntry entry = tableMap.get(keyHash);
        if (entry == null) return false;

        // Mark as deleted but keep entry for flushing
        entry.columns = null;
        entry.dirty = true;
        entry.version++;
        return true;
    }

    public Map<String, Map<String, CacheEntry>> raw() {
        return store;
    }

    private String hash(Map<String, Object> pk) {
        return pk.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
    }
}
