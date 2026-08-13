package org.ordermgmt.fulfillment.legacy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.stereotype.Component;

/**
 * Configuration of the legacy fulfillment drop folder, the Spring equivalent of the
 * {@code path} attribute of the Mule file outbound endpoint.
 */
@Component
@ConfigurationProperties(prefix = "legacy.fulfillment")
public class LegacyFulfillmentProperties {

    private String outputDirectory = "output";

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(@DefaultValue("output") String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}
