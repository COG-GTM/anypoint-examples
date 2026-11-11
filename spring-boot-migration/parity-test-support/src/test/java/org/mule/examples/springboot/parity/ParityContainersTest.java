package org.mule.examples.springboot.parity;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Smoke test for the shared Testcontainers setup (MySQL 5.7, as used by the Mule DB examples). */
@Testcontainers
class ParityContainersTest {

    @Container
    static final MySQLContainer<?> MYSQL = ParityContainers.mysql();

    @Test
    void mysqlContainerAcceptsQueries() throws Exception {
        try (Connection c = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             ResultSet rs = c.createStatement().executeQuery("SELECT VERSION()")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).startsWith("5.7");
        }
    }
}
