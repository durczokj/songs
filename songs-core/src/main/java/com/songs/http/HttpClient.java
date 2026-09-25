package com.songs.http;

import java.io.IOException;
import java.time.Duration;

public interface HttpClient {
  String get(String uri, Duration timeout) throws IOException;

  default String post(String uri, String jsonBody, Duration timeout) throws IOException {
    throw new UnsupportedOperationException("POST not supported");
  }
}
