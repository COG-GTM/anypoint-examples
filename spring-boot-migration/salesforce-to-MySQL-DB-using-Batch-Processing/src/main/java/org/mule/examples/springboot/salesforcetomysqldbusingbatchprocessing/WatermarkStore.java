package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Persistent store for poll watermarks (the Mule {@code watermark} element keeps its value in the
 * persistent object store, so it survives restarts). One row per watermark in table {@code watermark}.
 */
@Component
public class WatermarkStore {

    private final JdbcTemplate jdbc;

    public WatermarkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute("CREATE TABLE IF NOT EXISTS watermark (name VARCHAR(100) NOT NULL PRIMARY KEY, watermark_value VARCHAR(100) NOT NULL)");
    }

    public Optional<String> get(String name) {
        return jdbc.queryForList("SELECT watermark_value FROM watermark WHERE name = ?", String.class, name).stream().findFirst();
    }

    public void put(String name, String value) {
        if (jdbc.update("UPDATE watermark SET watermark_value = ? WHERE name = ?", value, name) == 0) {
            jdbc.update("INSERT INTO watermark (name, watermark_value) VALUES (?, ?)", name, value);
        }
    }

    public void clear(String name) {
        jdbc.update("DELETE FROM watermark WHERE name = ?", name);
    }
}
