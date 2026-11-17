package com.schoolos.webapi.config;

import com.schoolos.application.ports.AppVersionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/** Version from the Maven build (META-INF/build-info.properties); a fixed marker when run without it. */
@Component
public class BuildInfoVersionProvider implements AppVersionProvider {

    static final String UNKNOWN_VERSION = "0.0.0-dev";

    private final ObjectProvider<BuildProperties> buildProperties;

    public BuildInfoVersionProvider(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public String version() {
        BuildProperties properties = buildProperties.getIfAvailable();
        return properties == null ? UNKNOWN_VERSION : properties.getVersion();
    }
}
