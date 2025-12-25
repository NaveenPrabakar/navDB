package cachedb;

import java.util.HashMap;
import java.util.Map;

final class SimpleCodec {

    private SimpleCodec() {}

    // "{id=1, name=Alice}"
    static Map<String, Object> parseMap(String s) {
        Map<String, Object> map = new HashMap<>();

        s = s.trim();
        if (s.equals("{}")) return map;

        s = s.substring(1, s.length() - 1); // remove { }

        for (String entry : s.split(", ")) {
            String[] kv = entry.split("=", 2);
            map.put(kv[0], kv[1]);
        }

        return map;
    }
}
