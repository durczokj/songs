package com.songs.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;

final class SongsVersionProvider implements CommandLine.IVersionProvider {
  @Override
  public String[] getVersion() throws IOException {
    Properties properties = new Properties();
    try (InputStream stream =
        SongsVersionProvider.class.getResourceAsStream("/version.properties")) {
      if (stream == null) {
        throw new IOException("Missing version.properties");
      }
      properties.load(stream);
    }
    return new String[] {"songs " + properties.getProperty("version")};
  }
}
