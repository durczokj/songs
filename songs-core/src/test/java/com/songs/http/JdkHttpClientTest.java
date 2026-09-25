package com.songs.http;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
