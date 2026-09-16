# songs

A CLI that syncs a playlist (e.g. Apple Music) to a local folder, downloading missing tracks and removing extras.

## Install

Install the CLI with Homebrew:

```bash
brew tap durczokj/songs
brew install songs
brew install ffmpeg yt-dlp node
songs setup-browser
```

Check everything is installed:

```bash
songs doctor
```

If the browser needs to be installed or repaired:

```bash
songs setup-browser
```

## Usage

```bash
songs show file:///Users/you/Music
songs sync --dry-run "https://music.apple.com/..." "file:///Users/you/Music"
songs sync "https://music.apple.com/..." "file:///Users/you/Music"
```

`sync` makes the target match the source: it downloads missing tracks and
**deletes** extra ones. Always run `--dry-run` first.
