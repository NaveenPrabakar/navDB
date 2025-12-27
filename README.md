# CacheDB

A high-performance, write-behind caching layer for relational databases with automatic persistence, write-ahead logging (WAL), and crash recovery.

## Overview

CacheDB provides an in-memory cache with automatic write-behind persistence to a relational database. It features:

- **Write-behind caching**: Writes are immediately cached and asynchronously flushed to the database
- **Write-Ahead Logging (WAL)**: All writes are logged to disk for durability and crash recovery
- **Automatic schema detection**: Dynamically discovers table schemas and primary keys
- **TTL-based expiration**: Configurable time-to-live for cache entries
- **Crash recovery**: Automatically recovers unflushed writes from WAL on startup
- **Multi-table support**: Handles multiple tables with different schemas
- **Composite primary keys**: Supports tables with composite primary keys

## Architecture

```
┌─────────────┐
│ Application │
└──────┬──────┘
       │
       ▼
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│   CacheDB API   │────▶│  CacheStore  │────▶│   Database  │
│   (set/get)     │     │  (in-memory) │     │  (MySQL)    │
└────────┬────────┘     └──────┬───────┘     └─────────────┘
         │                     │
         │                     ▼
         │            ┌─────────────────┐
         │            │ ExpirationMgr    │
         │            │ (TTL cleanup)   │
         │            └────────┬─────────┘
         │                     │
         ▼                     ▼
┌──────────────┐      ┌─────────────────┐
│  WALWriter   │      │  FlushManager   │
│  (durability)│      │ (async flush)   │
└──────────────┘      └─────────────────┘
```

### Key Components

- **CacheDB**: Main API for setting and getting cached data
- **CacheStore**: In-memory cache with TTL-based expiration
- **FlushManager**: Asynchronously flushes expired cache entries to the database
- **ExpirationManager**: Monitors cache entries and triggers flushes when TTL expires
- **WALWriter/WALReader**: Write-ahead log for durability and recovery
- **SchemaRegistry**: Automatically discovers database table schemas and primary keys
- **SqlBuilder**: Generates SQL INSERT/UPDATE statements dynamically

## Features

### Write-Behind Persistence

Writes and deletes are immediately cached in memory and asynchronously flushed to the database when entries expire. This provides:

- **Low latency**: Reads, writes, and deletes are served from memory
- **High throughput**: Batches writes and deletes to the database
- **Durability**: All operations are logged to WAL before being cached

### Write-Ahead Logging (WAL)

Every write and delete operation is logged to a persistent WAL file (`logs/wal.log`) before being cached:

- **Crash recovery**: On startup, CacheDB replays all unflushed operations from WAL
- **Durability**: Writes and deletes survive application crashes
- **Checkpointing**: After successful database flushes, the WAL is truncated

### Automatic Schema Detection

CacheDB automatically discovers table schemas from the database:

- Detects primary keys (single or composite)
- Discovers column types
- Generates appropriate SQL statements dynamically
- Handles schema mismatches gracefully

## Usage

### Basic Setup

```java
import cachedb.CacheDB;
import cachedb.SimpleDataSource;
import javax.sql.DataSource;
import java.util.Map;

// Create a data source
DataSource ds = new SimpleDataSource(
    "jdbc:mysql://localhost:3306/cachedb",
    "root",
    "password"
);

// Build CacheDB instance
CacheDB cache = CacheDB.builder()
    .dataSource(ds)
    .ttlSeconds(2)  // Cache entries expire after 2 seconds
    .build();
```

### Writing Data

```java
// Set data in cache
cache.set(
    "users",                           // table name
    Map.of("id", 1),                   // primary key
    Map.of("name", "Alice",            // columns
           "email", "alice@test.com")
);
```

### Reading Data

```java
// Get data from cache
Map<String, Object> user = cache.get(
    "users",
    Map.of("id", 1)
);
// Returns: {name=Alice, email=alice@test.com}
```

### Deleting Data

```java
// Delete data from cache
cache.delete(
    "users",
    Map.of("id", 1)
);
// Entry is marked for deletion and will be removed from database on flush
```

### Multiple Tables

```java
// Cache supports multiple tables
cache.set("orders", 
    Map.of("order_id", 100),
    Map.of("status", "PAID", "total", 99.50)
);

cache.set("users",
    Map.of("id", 2),
    Map.of("name", "Bob")
);
```

### Composite Primary Keys

```java
// Support for composite primary keys
cache.set("order_items",
    Map.of("order_id", 100, "item_id", 3),  // composite key
    Map.of("qty", 2, "price", 19.99)
);

Map<String, Object> item = cache.get(
    "order_items",
    Map.of("order_id", 100, "item_id", 3)
);
```

### Manual Checkpointing

```java
// Manually checkpoint WAL after important operations
cache.checkpoint();
```

## How It Works

### Write Flow

1. Application calls `cache.set(table, primaryKey, columns)`
2. Write is immediately appended to WAL (durable)
3. Data is cached in memory with expiration timestamp
4. Application continues without waiting for database

### Delete Flow

1. Application calls `cache.delete(table, primaryKey)`
2. Delete is immediately appended to WAL (durable)
3. Entry is marked as deleted in cache (columns set to null)
4. Application continues without waiting for database

### Read Flow

1. Application calls `cache.get(table, primaryKey)`
2. CacheStore checks if entry exists and hasn't expired
3. Returns cached data immediately (no database query)

### Flush Flow

1. ExpirationManager periodically checks cache entries
2. When TTL expires, entry is marked for flushing
3. FlushManager asynchronously:
   - For updates: writes to database using UPSERT
   - For deletes: executes DELETE statement
4. After successful flush, WAL is checkpointed (truncated)

### Recovery Flow

1. On startup, CacheDB checks for existing WAL file
2. WALReader replays all PUT and DELETE operations from WAL
3. Recovered entries are loaded back into cache (PUT) or marked as deleted (DELETE)
4. Normal operation resumes

## Requirements

- **Java 17+**
- **MySQL 8.0+** (or compatible database)
- **Maven 3.6+** (for building)

## Building

```bash
# Compile and run tests
mvn clean test

# Build executable JAR
mvn clean package

# Run the demo
java -jar target/cache-db-1.0.jar
```

## Configuration

### TTL (Time-To-Live)

Control how long entries stay in cache before being flushed:

```java
CacheDB cache = CacheDB.builder()
    .dataSource(ds)
    .ttlSeconds(5)  // Entries expire after 5 seconds
    .build();
```

### Database Connection

Use any `javax.sql.DataSource` implementation:

```java
// Using SimpleDataSource (included)
DataSource ds = new SimpleDataSource(url, user, password);

// Or use your own DataSource (HikariCP, etc.)
DataSource ds = yourDataSource;
```

## Database Schema Requirements

- Tables must have a primary key (single or composite)
- Primary key columns cannot be null
- Tables are automatically discovered on first use

## WAL File Location

The write-ahead log is stored at: `logs/wal.log`

- Created automatically on first write
- Persists across application restarts
- Truncated after successful checkpoints

## Error Handling

- **Database failures**: Writes and deletes remain in WAL and cache, will be retried on next flush
- **WAL write failures**: Throws `RuntimeException` (operations cannot proceed without durability)
- **Schema errors**: Throws `RuntimeException` if table has no primary key
- **Recovery errors**: Corrupted WAL entries are skipped (safe recovery)

## Thread Safety

- CacheDB operations are thread-safe
- Multiple threads can safely call `set()` and `get()` concurrently
- Flush and expiration managers run in separate background threads

## Performance Considerations

- **Memory usage**: All cached data is held in memory
- **WAL growth**: WAL grows until checkpoints occur (after successful flushes)
- **Flush latency**: Database writes happen asynchronously (non-blocking)
- **Schema caching**: Table schemas are cached after first discovery

## Limitations

- No cache eviction policy (only TTL-based expiration)
- No cache size limits
- No distributed caching (single JVM only)
- No query support (only primary key lookups)
- WAL is not compressed (can grow large with many writes)

## Example Scenarios

### Scenario 1: Basic Lifecycle

```java
// Write data
cache.set("users", Map.of("id", 1), 
          Map.of("name", "Alice", "email", "alice@test.com"));

// Read immediately (from cache)
Map<String, Object> user = cache.get("users", Map.of("id", 1));

// Update data
cache.set("users", Map.of("id", 1),
          Map.of("name", "Alice-v2", "email", "alice@new.com"));

// After TTL expires, data is flushed to database
Thread.sleep(3000);
// Entry is now in database, removed from cache
```

### Scenario 2: Write Burst

```java
// Write many entries quickly
for (int i = 0; i < 1000; i++) {
    cache.set("users", 
        Map.of("id", i),
        Map.of("name", "User-" + i)
    );
}
// All writes are cached immediately
// Flushed to database asynchronously when TTL expires
```

### Scenario 3: Crash Recovery

```java
// Application crashes after writes
cache.set("users", Map.of("id", 1), Map.of("name", "Alice"));
// ... crash ...

// On restart, CacheDB automatically recovers from WAL
CacheDB cache2 = CacheDB.builder()...
    .build();
// All unflushed writes are restored to cache
```

### Scenario 4: Delete Operations

```java
// Write data
cache.set("users", Map.of("id", 1), Map.of("name", "Alice"));

// Delete data
cache.delete("users", Map.of("id", 1));

// Entry is marked as deleted in cache
// Will be removed from database when TTL expires and flush occurs
```

## Testing

Run the test suite:

```bash
mvn test
```

Test coverage includes:
- Basic lifecycle operations
- Multi-table support
- Composite primary keys
- Write bursts
- Crash recovery
- Checkpointing
- Idempotency


## Contributing

This is a personal project. Feel free to fork and modify for your own use.

