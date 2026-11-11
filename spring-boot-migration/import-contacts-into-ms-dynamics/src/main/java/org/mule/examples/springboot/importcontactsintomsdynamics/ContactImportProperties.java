package org.mule.examples.springboot.importcontactsintomsdynamics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The Mule {@code file:inbound-endpoint} attributes: {@code path}, {@code moveToDirectory}, {@code pollingFrequency}. */
@ConfigurationProperties(prefix = "contacts")
public record ContactImportProperties(String inputDir, String outputDir, long pollingFrequency) {
}
