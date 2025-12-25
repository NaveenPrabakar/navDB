package cachedb;

public final class LogRecord {

    public static final int MAGIC = 0xCAFEBABE;

    private final LogType type;
    private final byte[] key;
    private final byte[] value; // null for DELETE

    private LogRecord(LogType type, byte[] key, byte[] value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public static LogRecord put(byte[] key, byte[] value) {
        return new LogRecord(LogType.PUT, key, value);
    }

    public static LogRecord delete(byte[] key) {
        return new LogRecord(LogType.DELETE, key, null);
    }

    public LogType type() {
        return type;
    }

    public byte[] key() {
        return key;
    }

    public byte[] value() {
        return value;
    }
}
