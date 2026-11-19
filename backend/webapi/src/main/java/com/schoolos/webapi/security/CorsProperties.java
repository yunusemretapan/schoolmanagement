package com.schoolos.webapi.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API from another origin (spec 0003, Req. 12): the Expo web app in
 * development. Empty in production by default: every cross-origin browser request is refused.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::strip)
                        .filter(o -> !o.isEmpty())
                        .toList();
    }
}
