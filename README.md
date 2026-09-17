# songs

A CLI that syncs a playlist (e.g. Apple Music) to a local folder, downloading missing tracks and removing extras.

## Install

On a new Mac, install Homebrew first. Follow the instructions at
[brew.sh](https://brew.sh), then run:

```bash
brew tap durczokj/songs
brew install songs
brew install ffmpeg yt-dlp node
songs setup-browser
```

`brew install songs` installs Java 21. The other tools are installed
separately because Homebrew may otherwise try to compile large dependency trees, especially on Intel Macs.

Check everything is installed:

```bash
songs doctor
```

If the browser needs to be installed or repaired later:

```bash
songs setup-browser
```

## Usage

```bash
songs show file:///Users/you/Music
songs jobs run --dry-run --source "https://music.apple.com/..." --target "file:///Users/you/Music"
songs jobs run --source "https://music.apple.com/..." --target "file:///Users/you/Music"

songs jobs create favourite "https://music.apple.com/..." "file:///Users/you/Music"
songs jobs run favourite
```

`jobs run` makes the target match the source: it downloads missing tracks and
**deletes** extra ones. Saved jobs live in `~/.songs/config.json`; set
`SONGS_CONFIG` to use another config file. Always run `--dry-run` first.
