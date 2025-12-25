package cachedb;


public enum LogType {
    PUT((byte) 1),
    DELETE((byte) 2);

    private final byte code;

    LogType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static LogType fromCode(byte code) {
        for (LogType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown log type: " + code);
    }
}


