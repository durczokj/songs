package com.songs.matching;

import com.songs.model.Track;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class ExactTrackMatcher implements TrackMatcher {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public boolean matches(Track a, Track b) {
        if (a.isrc() != null && b.isrc() != null) {
            return a.isrc().equals(b.isrc());
        }
        if (isBlank(a.title()) || isBlank(a.artist()) || isBlank(b.title()) || isBlank(b.artist())) {
            return false;
        }
        return normalize(a.title()).equals(normalize(b.title()))
            && normalize(a.artist()).equals(normalize(b.artist()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
        String withoutPunctuation = NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase()).replaceAll("");
        return WHITESPACE.matcher(withoutPunctuation).replaceAll(" ").trim();
    }
}
