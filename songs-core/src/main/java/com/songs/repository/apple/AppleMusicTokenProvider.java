package com.songs.repository.apple;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Captures Apple Music's web-player API bearer token by loading a playlist page in a
 * headless browser and sniffing it off an outgoing request. The token is generated
 * client-side by JavaScript, so it cannot be scraped from the plain HTML/JS source.
 */
public class AppleMusicTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(AppleMusicTokenProvider.class);
    private static final Pattern DEV_TOKEN = Pattern.compile("devToken=([^&]+)");

    /** Returns the "Bearer ..." token, or null if it could not be captured. */
    public String captureToken(String playlistUri) {
        logger.info("Capturing Apple Music authorization token for {}", playlistUri);
        // Playwright's sync API cannot run inside an already-running event loop (e.g. tests),
        // so isolate it in its own thread.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(() -> capture(playlistUri)).get();
        } catch (Exception e) {
            logger.warn("Could not capture Apple Music authorization token for {}: {}", playlistUri, e.getMessage());
            return null;
        } finally {
            executor.shutdown();
        }
    }

    private String capture(String playlistUri) {
        AtomicReference<String> token = new AtomicReference<>();
        // Prevent Playwright 1.61+ from auto-downloading Firefox/WebKit; we only need Chromium.
        Playwright.CreateOptions options = new Playwright.CreateOptions()
            .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
        try (Playwright playwright = Playwright.create(options)) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.onRequest(request -> {
                if (token.get() != null) {
                    return;
                }
                String url = request.url();
                if (url.contains("amp-api.music.apple.com")) {
                    String auth = request.headers().get("authorization");
                    if (auth != null) {
                        token.set(auth);
                        logger.debug("Captured Apple Music authorization header");
                        return;
                    }
                }
                Matcher matcher = DEV_TOKEN.matcher(url);
                if (matcher.find()) {
                    token.set("Bearer " + matcher.group(1));
                    logger.debug("Captured Apple Music developer token from request URL");
                }
            });
            page.navigate(playlistUri, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(4000);
            browser.close();
        }
        if (token.get() == null) {
            logger.warn("Apple Music page did not expose an authorization token for {}", playlistUri);
        }
        return token.get();
    }
}
