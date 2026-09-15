package com.songs.matching;

import com.songs.model.Track;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactTrackMatcherTest {

    private final ExactTrackMatcher matcher = new ExactTrackMatcher();

    @Test
    void matchesOnIsrcWhenBothHaveIt() {
        Track a = new Track("Song A", "Artist A", null, null, "USRC17607839");
        Track b = new Track("Completely Different", "Someone Else", null, null, "USRC17607839");

        assertTrue(matcher.matches(a, b));
    }

    @Test
    void doesNotMatchDifferentIsrc() {
        Track a = new Track("Song A", "Artist A", null, null, "USRC17607839");
        Track b = new Track("Song A", "Artist A", null, null, "USRC17607840");

        assertFalse(matcher.matches(a, b));
    }

    @Test
    void matchesTitleCaseInsensitively() {
        Track a = new Track("Bohemian Rhapsody", "Queen", null, null, null);
        Track b = new Track("BOHEMIAN RHAPSODY", "queen", null, null, null);

        assertTrue(matcher.matches(a, b));
    }

    @Test
    void matchesIgnoringPunctuation() {
        Track a = new Track("Don't Stop Believin'", "Journey", null, null, null);
        Track b = new Track("Dont Stop Believin", "Journey", null, null, null);

        assertTrue(matcher.matches(a, b));
    }

    @Test
    void matchesIgnoringAccents() {
        Track a = new Track("Café", "Amélie", null, null, null);
        Track b = new Track("Cafe", "Amelie", null, null, null);

        assertTrue(matcher.matches(a, b));
    }

    @Test
    void matchesIgnoringExtraWhitespace() {
        Track a = new Track("Song  Title", "The   Artist", null, null, null);
        Track b = new Track("Song Title", "The Artist", null, null, null);

        assertTrue(matcher.matches(a, b));
    }

    @Test
    void doesNotMatchDifferentTitleOrArtistWhenNoIsrc() {
        Track a = new Track("Song A", "Artist A", null, null, null);
        Track b = new Track("Song B", "Artist A", null, null, null);

        assertFalse(matcher.matches(a, b));
    }

    @Test
    void fallsBackToTitleArtistWhenOnlyOneHasIsrc() {
        Track a = new Track("Song A", "Artist A", null, null, "USRC17607839");
        Track b = new Track("Song A", "Artist A", null, null, null);

        assertTrue(matcher.matches(a, b));
    }

    @Test
    void doesNotThrowOrMatchWhenFallbackMetadataIsIncomplete() {
        Track missingTitle = new Track(null, "Artist A", null, null, null);
        Track complete = new Track("Song A", "Artist A", null, null, null);

        assertFalse(matcher.matches(missingTitle, complete));
    }

    @Test
    void stillMatchesByIsrcWhenTitleOrArtistIsMissing() {
        Track a = new Track(null, null, null, null, "USRC17607839");
        Track b = new Track("Song A", "Artist A", null, null, "USRC17607839");

        assertTrue(matcher.matches(a, b));
    }
}
