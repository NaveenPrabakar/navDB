package cachedb;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class WALReader implements Iterable<LogRecord>, Closeable {

    private final FileChannel channel;

    public WALReader(Path walPath) throws IOException {
        if (!walPath.toFile().exists()) {
            this.channel = null;
        } else {
            this.channel = FileChannel.open(walPath, StandardOpenOption.READ);
        }
    }

    @Override
    public Iterator<LogRecord> iterator() {
        return new Iterator<>() {

            private long position = 0;
            private LogRecord next;

            @Override
            public boolean hasNext() {
                if (next != null) return true;
                next = readNext();
                return next != null;
            }

            @Override
            public LogRecord next() {
                if (!hasNext()) throw new NoSuchElementException();
                LogRecord r = next;
                next = null;
                return r;
            }

            private LogRecord readNext() {
                try {
                    if (channel == null || position >= channel.size()) {
                        return null;
                    }

                    // Read header
                    ByteBuffer header = ByteBuffer.allocate(8);
                    channel.position(position);
                    if (channel.read(header) < 8) return null;
                    header.flip();

                    int magic = header.getInt();
                    int totalLen = header.getInt();

                    if (magic != LogRecord.MAGIC || totalLen <= 0) {
                        return null; // corruption → stop
                    }

                    if (position + totalLen > channel.size()) {
                        return null; // partial record → stop
                    }

                    ByteBuffer recordBuf = ByteBuffer.allocate(totalLen - 8);
                    channel.read(recordBuf);
                    recordBuf.flip();

                    position += totalLen;
                    return LogSerializer.deserialize(recordBuf);

                } catch (IOException e) {
                    return null; // safe stop on any IO issue
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        if (channel != null) channel.close();
    }
}
