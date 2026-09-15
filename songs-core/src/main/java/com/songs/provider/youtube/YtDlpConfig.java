package com.songs.provider.youtube;

import java.time.Duration;

public record YtDlpConfig(String binaryPath, String outputTemplate, Duration timeout) {

    public YtDlpConfig() {
        this("yt-dlp", "%(id)s.%(ext)s", Duration.ofMinutes(5));
    }
}
