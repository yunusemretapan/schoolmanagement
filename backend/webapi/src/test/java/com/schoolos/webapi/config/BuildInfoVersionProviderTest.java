package com.schoolos.webapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

class BuildInfoVersionProviderTest {

    @Test
    void usesTheBuildVersion() {
        Properties entries = new Properties();
        entries.setProperty("version", "0.1.0");

        assertThat(provider(new BuildProperties(entries)).version()).isEqualTo("0.1.0");
    }

    @Test
    void fallsBackWhenBuildInfoIsMissing() {
        assertThat(provider(null).version()).isEqualTo(BuildInfoVersionProvider.UNKNOWN_VERSION);
    }

    @SuppressWarnings("unchecked")
    private static BuildInfoVersionProvider provider(BuildProperties properties) {
        ObjectProvider<BuildProperties> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(properties);
        return new BuildInfoVersionProvider(objectProvider);
    }
}
