package org.mule.examples.springboot.parity;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers factories with the image versions the migration standardises on. Prefer
 * declaring them as {@code @Bean @ServiceConnection} in a {@code @TestConfiguration} (Spring Boot
 * 3.1+) or as {@code @Container static} fields; specialised modules (rabbitmq, mongodb, ...) can
 * be added per migrated module using the image names below.
 */
public final class ParityContainers {

    /** Matches the MySQL 5.x the Mule examples were written against. */
    public static final String MYSQL_IMAGE = "mysql:5.7";
    public static final String ACTIVEMQ_IMAGE = "apache/activemq-classic:5.18.3";
    public static final String RABBITMQ_IMAGE = "rabbitmq:3.13-management";
    public static final String MONGODB_IMAGE = "mongo:6.0";
    public static final String GREENMAIL_IMAGE = "greenmail/standalone:2.0.1";
    public static final String OPENLDAP_IMAGE = "osixia/openldap:1.5.0";
    public static final String SFTP_IMAGE = "atmoz/sftp:alpine";

    private ParityContainers() {
    }

    public static MySQLContainer<?> mysql() {
        return new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE).asCompatibleSubstituteFor("mysql"))
                .withDatabaseName("parity")
                .withUsername("parity")
                .withPassword("parity");
    }

    /** ActiveMQ Classic: OpenWire on 61616, default admin/admin. */
    public static GenericContainer<?> activeMq() {
        return new GenericContainer<>(DockerImageName.parse(ACTIVEMQ_IMAGE))
                .withExposedPorts(61616)
                .waitingFor(Wait.forListeningPort());
    }

    /** RabbitMQ: AMQP on 5672, guest/guest. */
    public static GenericContainer<?> rabbitMq() {
        return new GenericContainer<>(DockerImageName.parse(RABBITMQ_IMAGE))
                .withExposedPorts(5672, 15672)
                .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));
    }

    public static GenericContainer<?> mongo() {
        return new GenericContainer<>(DockerImageName.parse(MONGODB_IMAGE))
                .withExposedPorts(27017)
                .waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
    }

    /** GreenMail: SMTP 3025, POP3 3110, IMAP 3143; any user/password auto-provisioned. */
    public static GenericContainer<?> greenMail() {
        return new GenericContainer<>(DockerImageName.parse(GREENMAIL_IMAGE))
                .withEnv("GREENMAIL_OPTS", "-Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 -Dgreenmail.auth.disabled")
                .withExposedPorts(3025, 3110, 3143)
                .waitingFor(Wait.forListeningPorts(3025, 3110, 3143));
    }

    /** SFTP server with user {@code parity}/{@code parity} and writable {@code upload} dir. */
    public static GenericContainer<?> sftp() {
        return new GenericContainer<>(DockerImageName.parse(SFTP_IMAGE))
                .withCommand("parity:parity:::upload")
                .withExposedPorts(22)
                .waitingFor(Wait.forListeningPort());
    }
}
