package com.songs.repository.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.songs.model.Playlist;
import com.songs.model.Track;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches Apple Music playlist metadata and all tracks via the catalog API.
 */
public class ApplePlaylistPaginator {

    private static final Logger logger = LoggerFactory.getLogger(ApplePlaylistPaginator.class);
    private static final String CATALOG_HOST = "https://amp-api.music.apple.com";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public ApplePlaylistPaginator() {
        this(HttpClient.newHttpClient());
    }

    public ApplePlaylistPaginator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Playlist fetchPlaylist(String storefront, String playlistId, String bearerToken, String originalUri) throws IOException {
        logger.info("Fetching Apple Music catalog playlist {}", playlistId);
        URI initialUri = URI.create(CATALOG_HOST + "/v1/catalog/" + storefront + "/playlists/" + playlistId);
        String responseBody = sendGet(initialUri, bearerToken);

        JsonNode root = MAPPER.readTree(responseBody);
        JsonNode playlistNode = root.path("data").path(0);
        if (playlistNode.isMissingNode() || playlistNode.isNull()) {
            throw new IOException("Playlist not found: " + playlistId);
        }

        String playlistName = playlistNode.path("attributes").path("name").asText("Untitled Playlist");
        JsonNode tracksRelationship = playlistNode.path("relationships").path("tracks");

        List<Track> tracks = new ArrayList<>();
        JsonNode initialItems = tracksRelationship.path("data");
        if (initialItems.isArray()) {
            for (JsonNode item : initialItems) {
                tracks.add(toTrack(item));
            }
        }

        String nextPath = tracksRelationship.path("next").asText(null);
        while (nextPath != null && !nextPath.isBlank()) {
            logger.debug("Fetching next Apple Music playlist page for {}", playlistId);
            URI nextUri = URI.create(CATALOG_HOST + nextPath);
            String nextBody = sendGet(nextUri, bearerToken);
            JsonNode nextRoot = MAPPER.readTree(nextBody);
            JsonNode nextItems = nextRoot.path("data");
            if (!nextItems.isArray() || nextItems.isEmpty()) {
                break;
            }
            for (JsonNode item : nextItems) {
                tracks.add(toTrack(item));
            }
            nextPath = nextRoot.path("next").asText(null);
        }

        Playlist playlist = new Playlist(originalUri, tracks, playlistName);
        logger.info("Fetched {} tracks for Apple Music playlist {}", tracks.size(), playlistName);
        return playlist;
    }

    private String sendGet(URI uri, String bearerToken) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", bearerToken)
                .header("Origin", "https://music.apple.com")
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching Apple Music catalog data", e);
        }

        if (response.statusCode() >= 400) {
            logger.warn("Apple Music catalog request {} returned status {}", uri, response.statusCode());
            throw new IOException("Catalog API request failed with HTTP " + response.statusCode() + " for URI: " + uri);
        }
        return response.body();
    }

    Track toTrack(JsonNode item) {
        JsonNode attributes = item.path("attributes");
        Integer durationSec = attributes.has("durationInMillis")
                ? attributes.get("durationInMillis").asInt() / 1000
                : null;
        return new Track(
                attributes.path("name").asText(null),
                attributes.path("artistName").asText(null),
                attributes.path("albumName").asText(null),
                durationSec,
                attributes.path("isrc").asText(null)
        );
    }
}
