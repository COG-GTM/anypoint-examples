package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Parity over plain FTP (the protocol the Mule app uses), against an embedded Apache FtpServer with the Mule
 * endpoint's user {@code demo}/{@code demo} and an {@code /Upload/} directory.
 */
class FtpUploadParityTest extends AbstractUploadParityTest {

    static final Path FTP_HOME;
    static final int FTP_PORT;
    static final FtpServer FTP_SERVER;

    static {
        try {
            FTP_HOME = Files.createTempDirectory("ftp-home");
            Files.createDirectories(FTP_HOME.resolve("Upload"));
            try (ServerSocket socket = new ServerSocket(0)) {
                FTP_PORT = socket.getLocalPort();
            }
            FtpServerFactory serverFactory = new FtpServerFactory();
            ListenerFactory listener = new ListenerFactory();
            listener.setPort(FTP_PORT);
            serverFactory.addListener("default", listener.createListener());
            var userManager = new PropertiesUserManagerFactory().createUserManager();
            BaseUser user = new BaseUser();
            user.setName("demo");
            user.setPassword("demo");
            user.setHomeDirectory(FTP_HOME.toString());
            user.setAuthorities(List.<Authority>of(new WritePermission()));
            userManager.save(user);
            serverFactory.setUserManager(userManager);
            FTP_SERVER = serverFactory.createServer();
            FTP_SERVER.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void ftp(DynamicPropertyRegistry registry) {
        registry.add("ftp.protocol", () -> "ftp");
        registry.add("ftp.host", () -> "localhost");
        registry.add("ftp.port", () -> FTP_PORT);
    }

    @AfterAll
    static void stopServer() {
        FTP_SERVER.stop();
    }

    @Override
    protected byte[] readRemoteFile(String path) throws IOException {
        return Files.readAllBytes(FTP_HOME.resolve(path.replaceFirst("^/+", "")));
    }
}
