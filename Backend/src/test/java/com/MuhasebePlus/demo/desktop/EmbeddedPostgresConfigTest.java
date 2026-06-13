package com.MuhasebePlus.demo.desktop;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * EmbeddedPostgresConfig (desktop profili) için birim testleri.
 *
 * Gerçek bir PostgreSQL süreci başlatmamak için {@code EmbeddedPostgres.builder()}
 * statik fabrika metodu mock'lanır; {@code user.home} sistem özelliği geçici bir
 * dizine yönlendirilerek dosya sistemi mantığı (veri dizini oluşturma, bayat kilit
 * dosyasını silme) gerçek dosyalar üzerinde doğrulanır.
 */
class EmbeddedPostgresConfigTest {

    @TempDir
    Path tempHome;

    private EmbeddedPostgresConfig config;
    private String originalUserHome;
    private Path expectedDataDir;

    @BeforeEach
    void setUp() {
        config = new EmbeddedPostgresConfig();
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());
        expectedDataDir = tempHome.resolve("MuhasebePlus").resolve("data");
    }

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        } else {
            System.clearProperty("user.home");
        }
    }

    /** Builder zincirinin tüm fluent çağrıları kendisini, start() ise mock PG'yi döndürür. */
    private EmbeddedPostgres.Builder selfReturningBuilder(EmbeddedPostgres started) throws IOException {
        EmbeddedPostgres.Builder builder =
                mock(EmbeddedPostgres.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        when(builder.start()).thenReturn(started);
        return builder;
    }

    @Test
    void embeddedPostgres_createsDataDirectoryAndConfiguresBuilder() throws Exception {
        EmbeddedPostgres pg = mock(EmbeddedPostgres.class);
        EmbeddedPostgres.Builder builder = selfReturningBuilder(pg);

        try (MockedStatic<EmbeddedPostgres> mocked = mockStatic(EmbeddedPostgres.class)) {
            mocked.when(EmbeddedPostgres::builder).thenReturn(builder);

            EmbeddedPostgres result = config.embeddedPostgres();

            assertThat(result).isSameAs(pg);
            // Veri dizini fiilen oluşturulmalı
            assertThat(Files.isDirectory(expectedDataDir)).isTrue();

            // Builder doğru parametrelerle yapılandırılmalı
            verify(builder).setDataDirectory(new File(expectedDataDir.toString()));
            verify(builder).setCleanDataDirectory(false);
            verify(builder).setLocaleConfig("locale", "C");
            verify(builder).setLocaleConfig("lc-messages", "C");
            verify(builder).start();
        }
    }

    @Test
    void embeddedPostgres_deletesStaleLockFileWhenPresent() throws Exception {
        // Önceki çalışmadan kalmış bayat postmaster.pid kilit dosyasını hazırla
        Files.createDirectories(expectedDataDir);
        Path pidFile = expectedDataDir.resolve("postmaster.pid");
        Files.writeString(pidFile, "12345");
        assertThat(Files.exists(pidFile)).isTrue();

        EmbeddedPostgres pg = mock(EmbeddedPostgres.class);
        EmbeddedPostgres.Builder builder = selfReturningBuilder(pg);

        try (MockedStatic<EmbeddedPostgres> mocked = mockStatic(EmbeddedPostgres.class)) {
            mocked.when(EmbeddedPostgres::builder).thenReturn(builder);

            EmbeddedPostgres result = config.embeddedPostgres();

            assertThat(result).isSameAs(pg);
            // Bayat kilit dosyası silinmiş olmalı
            assertThat(Files.exists(pidFile)).isFalse();
        }
    }

    @Test
    void embeddedPostgres_whenNoStaleLockFile_startsWithoutDeleting() throws Exception {
        EmbeddedPostgres pg = mock(EmbeddedPostgres.class);
        EmbeddedPostgres.Builder builder = selfReturningBuilder(pg);

        try (MockedStatic<EmbeddedPostgres> mocked = mockStatic(EmbeddedPostgres.class)) {
            mocked.when(EmbeddedPostgres::builder).thenReturn(builder);

            EmbeddedPostgres result = config.embeddedPostgres();

            assertThat(result).isSameAs(pg);
            assertThat(Files.exists(expectedDataDir.resolve("postmaster.pid"))).isFalse();
            verify(builder).start();
        }
    }

    @Test
    void embeddedPostgres_propagatesStartFailure() throws Exception {
        EmbeddedPostgres.Builder builder =
                mock(EmbeddedPostgres.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        when(builder.start()).thenThrow(new IOException("postgres başlatılamadı"));

        try (MockedStatic<EmbeddedPostgres> mocked = mockStatic(EmbeddedPostgres.class)) {
            mocked.when(EmbeddedPostgres::builder).thenReturn(builder);

            assertThatThrownBy(() -> config.embeddedPostgres())
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("postgres başlatılamadı");
        }
    }

    @Test
    void dataSource_returnsPostgresDatabaseFromEmbeddedPostgres() {
        EmbeddedPostgres pg = mock(EmbeddedPostgres.class);
        DataSource expected = mock(DataSource.class);
        when(pg.getPostgresDatabase()).thenReturn(expected);

        DataSource result = config.dataSource(pg);

        assertThat(result).isSameAs(expected);
        verify(pg).getPostgresDatabase();
    }
}
