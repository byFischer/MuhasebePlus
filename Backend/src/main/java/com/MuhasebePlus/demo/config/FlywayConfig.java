package com.MuhasebePlus.demo.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * Run Flyway "repair" before every migrate so that checksum mismatches caused by
     * edited (already-applied) migration scripts do not block application startup.
     * Spring Boot has no built-in "repair-on-migrate" property, so it is done here.
     */
    @Bean
    public FlywayMigrationStrategy repairBeforeMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
