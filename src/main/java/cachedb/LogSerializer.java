package cachedb;


import java.nio.ByteBuffer;

public final class LogSerializer {

    private LogSerializer() {}

    /*
     * Format:
     * [MAGIC:int]
     * [TOTAL_LENGTH:int]
     * [TYPE:byte]
     * [KEY_LEN:int]
     * [KEY:bytes]
     * [VALUE_LEN:int] (-1 for DELETE)
     * [VALUE:bytes]
     */
    public static ByteBuffer serialize(LogRecord r) {
        int keyLen = r.key().length;
        int valueLen = (r.value() == null) ? -1 : r.value().length;

        int totalLen =
                Integer.BYTES + // MAGIC
                        Integer.BYTES + // TOTAL_LENGTH
                        Byte.BYTES +
                        Integer.BYTES + keyLen +
                        Integer.BYTES + (valueLen > 0 ? valueLen : 0);

        ByteBuffer buf = ByteBuffer.allocate(totalLen);

        buf.putInt(LogRecord.MAGIC);
        buf.putInt(totalLen);
        buf.put(r.type().code());

        buf.putInt(keyLen);
        buf.put(r.key());

        buf.putInt(valueLen);
        if (valueLen > 0) {
            buf.put(r.value());
        }

        buf.flip();
        return buf;
    }

    public static LogRecord deserialize(ByteBuffer buf) {
        byte typeCode = buf.get();
        LogType type = LogType.fromCode(typeCode);

        int keyLen = buf.getInt();
        byte[] key = new byte[keyLen];
        buf.get(key);

        int valueLen = buf.getInt();
        byte[] value = null;

        if (valueLen >= 0) {
            value = new byte[valueLen];
            buf.get(value);
        }

        return (type == LogType.PUT)
                ? LogRecord.put(key, value)
                : LogRecord.delete(key);
    }
}
