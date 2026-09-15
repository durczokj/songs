package com.songs.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdkHttpClient implements HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(JdkHttpClient.class);

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final java.net.http.HttpClient delegate = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String get(String uri, Duration timeout) throws IOException {
        logger.debug("HTTP GET {}", uri);
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
            .header("User-Agent", DEFAULT_USER_AGENT)
            .timeout(timeout)
            .GET()
            .build();

        try {
            HttpResponse<String> response = delegate.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                logger.warn("HTTP GET {} returned status {}", uri, response.statusCode());
                throw new IOException("HTTP " + response.statusCode() + " fetching " + uri);
            }
            logger.debug("HTTP GET {} returned status {}", uri, response.statusCode());
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("HTTP GET interrupted for {}", uri);
            throw new IOException("Interrupted while fetching " + uri, e);
        }
    }

    @Override
    public String post(String uri, String jsonBody, Duration timeout) throws IOException {
        logger.debug("HTTP POST {}", uri);
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
            .header("Content-Type", "application/json")
            .header("User-Agent", DEFAULT_USER_AGENT)
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        try {
            HttpResponse<String> response = delegate.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                logger.warn("HTTP POST {} returned status {}", uri, response.statusCode());
                throw new IOException("HTTP " + response.statusCode() + " posting " + uri);
            }
            logger.debug("HTTP POST {} returned status {}", uri, response.statusCode());
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("HTTP POST interrupted for {}", uri);
            throw new IOException("Interrupted while posting to " + uri, e);
        }
    }
}
