package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the Mule {@code ftp:outbound-endpoint "Upload to FTP"}. {@code protocol} is new: {@code ftp} (the Mule
 * behaviour, default) or {@code sftp}.
 */
@ConfigurationProperties(prefix = "ftp")
public record FtpProperties(
        String protocol,
        String host,
        int port,
        String user,
        String password,
        String path,
        String outputPattern,
        boolean passive,
        boolean allowUnknownKeys) {
}
