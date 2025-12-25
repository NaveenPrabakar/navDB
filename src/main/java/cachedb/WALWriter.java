package cachedb;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static cachedb.LogSerializer.serialize;

public class WALWriter implements Closeable {

    private static volatile WALWriter INSTANCE;

    private final FileChannel channel;

    public WALWriter(Path path) throws IOException {
        channel = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
        INSTANCE = this;
    }

    public static WALWriter getInstance() {
        WALWriter w = INSTANCE;
        if (w == null) {
            throw new IllegalStateException("WALWriter not initialized");
        }
        return w;
    }

    public synchronized void append(LogRecord record) throws IOException {
        ByteBuffer buffer = serialize(record);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        channel.force(true);
    }

    public synchronized void sync() throws IOException {
        channel.force(true);
    }

    public synchronized void truncate() throws IOException {
        channel.truncate(0);
        channel.position(0);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
