package org.mule.examples.springboot.queryingamysqldatabase;

import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.ParityContainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class QueryingAMysqlDatabaseParityTest extends AbstractParityTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = ParityContainers.mysql().withInitScript("employees-seed.sql");

    @Override
    protected String fixtureDirectory() {
        return "parity/querying-a-mysql-database";
    }
}
