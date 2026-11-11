package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/** The Mule {@code ftp:connector} + {@code ftp:outbound-endpoint} as a Spring Integration {@link RemoteFileTemplate}. */
@Configuration(proxyBeanMethods = false)
class FtpUploadConfiguration {

    @Bean
    RemoteFileTemplate<?> uploadTemplate(FtpProperties ftp) {
        RemoteFileTemplate<?> template = "sftp".equalsIgnoreCase(ftp.protocol())
                ? new SftpRemoteFileTemplate(sftpSessionFactory(ftp))
                : new FtpRemoteFileTemplate(ftpSessionFactory(ftp));
        template.setRemoteDirectoryExpression(new LiteralExpression(ftp.path()));
        template.setFileNameGenerator(message -> ftp.outputPattern());
        // Mule writes the target file directly (no temporary name + rename).
        template.setUseTemporaryFileName(false);
        template.setAutoCreateDirectory(false);
        template.afterPropertiesSet();
        return template;
    }

    private static SessionFactory<org.apache.commons.net.ftp.FTPFile> ftpSessionFactory(FtpProperties ftp) {
        DefaultFtpSessionFactory factory = new DefaultFtpSessionFactory();
        factory.setHost(ftp.host());
        factory.setPort(ftp.port());
        factory.setUsername(ftp.user());
        factory.setPassword(ftp.password());
        // Mule 3 FTP connector defaults: passive mode, binary transfers.
        factory.setClientMode(ftp.passive() ? FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE
                : FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE);
        factory.setFileType(FTP.BINARY_FILE_TYPE);
        return factory;
    }

    private static DefaultSftpSessionFactory sftpSessionFactory(FtpProperties ftp) {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(ftp.host());
        factory.setPort(ftp.port());
        factory.setUser(ftp.user());
        factory.setPassword(ftp.password());
        factory.setAllowUnknownKeys(ftp.allowUnknownKeys());
        return factory;
    }
}
