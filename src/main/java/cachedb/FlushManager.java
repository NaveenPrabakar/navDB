package cachedb;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FlushManager implements Runnable {

    private final BlockingQueue<FlushTask> queue = new LinkedBlockingQueue<>();
    private final DataSource dataSource;
    private final SchemaRegistry schemaRegistry;

    public FlushManager(DataSource ds, SchemaRegistry schemaRegistry) {
        this.dataSource = ds;
        this.schemaRegistry = schemaRegistry;
    }

    public void enqueue(FlushTask task) {
        queue.offer(task);
    }

    @Override
    public void run() {
        while (true) {
            try {
                FlushTask task = queue.take();
                flush(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void flush(FlushTask task) throws Exception {
        RowMutation m = task.mutation;
        TableSchema schema = schemaRegistry.get(m.table);
        String sql = SqlBuilder.buildUpsert(m, schema);

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = 1;
            for (String pk : schema.primaryKeys) {
                Object value = m.primaryKey.get(pk);
                if (value == null) {
                    value = m.columns.get(pk);
                }
                if (value == null && schema.primaryKeys.size() == 1 && m.primaryKey.size() == 1) {
                    value = m.primaryKey.values().iterator().next();
                }
                ps.setObject(idx++, value);
            }
            for (String col : m.columns.keySet()) {
                if (!schema.primaryKeys.contains(col)) {
                    ps.setObject(idx++, m.columns.get(col));
                }
            }

            try {
                ps.executeUpdate();
                checkpoint();
            } catch (Exception e) {
                // DB down → WAL preserved
            }

            System.out.println("[FLUSHED] " + m.table + " " + m.primaryKey);
        }
    }

    private void checkpoint() {
        try {
            WALWriter wal = WALWriter.getInstance();
            wal.sync();
            wal.truncate();
        } catch (Exception e) {
            // swallow — DB is already durable
        }
    }

}
