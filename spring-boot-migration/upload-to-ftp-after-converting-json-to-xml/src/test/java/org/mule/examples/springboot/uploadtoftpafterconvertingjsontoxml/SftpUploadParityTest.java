package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import org.mule.examples.springboot.parity.ParityContainers;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;

/**
 * Same fixtures with {@code ftp.protocol=sftp}, against the SFTP Testcontainer image from {@link ParityContainers}
 * configured with the Mule endpoint's user ({@code demo}/{@code demo}) and directory ({@code /Upload/}). The uploaded
 * file is read back through an independent SFTP session.
 */
@Testcontainers
class SftpUploadParityTest extends AbstractUploadParityTest {

    @Container
    static final GenericContainer<?> SFTP = new GenericContainer<>(DockerImageName.parse(ParityContainers.SFTP_IMAGE))
            .withCommand("demo:demo:::Upload")
            .withExposedPorts(22)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void sftp(DynamicPropertyRegistry registry) {
        registry.add("ftp.protocol", () -> "sftp");
        registry.add("ftp.host", SFTP::getHost);
        registry.add("ftp.port", () -> SFTP.getMappedPort(22));
        registry.add("ftp.allow-unknown-keys", () -> "true");
    }

    @Override
    protected byte[] readRemoteFile(String path) {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(SFTP.getHost());
        factory.setPort(SFTP.getMappedPort(22));
        factory.setUser("demo");
        factory.setPassword("demo");
        factory.setAllowUnknownKeys(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SftpRemoteFileTemplate(factory).get(path, in -> in.transferTo(out));
        return out.toByteArray();
    }
}
