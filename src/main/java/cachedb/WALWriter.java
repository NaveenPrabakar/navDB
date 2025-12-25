package cachedb;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static cachedb.LogSerializer.serialize;

public class WALWriter implements Closeable {

    private final FileChannel channel;

    public WALWriter(Path path) throws IOException {
        channel = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
    }

    public synchronized void append(LogRecord record) throws IOException {
        ByteBuffer buffer = serialize(record);
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        channel.force(true); // fsync
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public void truncate() throws IOException {
        channel.truncate(0);
        channel.position(0);
    }

}

