package com.MuhasebePlus.demo.desktop;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Profile("desktop")
public class EmbeddedPostgresConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        Path dataDir = Paths.get(System.getProperty("user.home"), "MuhasebePlus", "data");
        Files.createDirectories(dataDir);

        // Stale lock file — önceki process temiz kapanmadıysa bırakıyor
        Path pidFile = dataDir.resolve("postmaster.pid");
        if (Files.exists(pidFile)) {
            Files.delete(pidFile);
        }

        return EmbeddedPostgres.builder()
                .setDataDirectory(dataDir.toFile())
                .setCleanDataDirectory(false)
                .setLocaleConfig("locale", "C")
                .setLocaleConfig("lc-messages", "C")
                .start();
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}
