package com.songs.http;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Hits the network; excluded from the default `mvn test` run (see pom.xml surefire config).
// Run with: mvn test -Dgroups=integration
@Tag("integration")
class JdkHttpClientTest {

    @Test
    void getsBodyFromLiveServer() throws Exception {
        JdkHttpClient client = new JdkHttpClient();

        String body = client.get("https://example.com", Duration.ofSeconds(10));

        assertTrue(body.contains("Example Domain"));
    }
}
