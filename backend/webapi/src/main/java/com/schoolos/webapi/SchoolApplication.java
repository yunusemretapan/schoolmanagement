package com.schoolos.webapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Scans all of {@code com.schoolos}: the persistence module's adapters are on the runtime classpath only, so webapi
 * never compiles against them (docs/architecture.md, Forbidden dependencies 3).
 */
@SpringBootApplication(scanBasePackages = "com.schoolos")
public class SchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolApplication.class, args);
    }
}
