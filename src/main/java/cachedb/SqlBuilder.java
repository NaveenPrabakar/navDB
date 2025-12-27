package cachedb;

import java.util.*;

public class SqlBuilder {

    public static String buildUpsert(RowMutation m, TableSchema schema) {

        List<String> cols = new ArrayList<>();
        cols.addAll(schema.primaryKeys);
        for (String col : m.columns.keySet()) {
            if (!schema.primaryKeys.contains(col)) {
                cols.add(col);
            }
        }

        String colList = String.join(", ", cols);
        String placeholders =
                String.join(", ", Collections.nCopies(cols.size(), "?"));

        String updates = m.columns.keySet().stream()
                .map(c -> c + " = VALUES(" + c + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        return "INSERT INTO " + m.table +
                " (" + colList + ") VALUES (" + placeholders + ")" +
                " ON DUPLICATE KEY UPDATE " + updates;
    }

    public static String buildDelete(RowMutation m, TableSchema schema) {
        String whereClause = schema.primaryKeys.stream()
                .map(pk -> pk + " = ?")
                .reduce((a, b) -> a + " AND " + b)
                .orElse("");

        return "DELETE FROM " + m.table + " WHERE " + whereClause;
    }
}
