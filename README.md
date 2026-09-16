# songs

A CLI that syncs a playlist (e.g. Apple Music) to a local folder, downloading missing tracks and removing extras.

## Setup

Requires: JDK 21, Maven, `ffmpeg`, `yt-dlp`, and `node` or `deno`.

```bash
brew install openjdk@21 maven ffmpeg yt-dlp node
git clone https://github.com/durczokj/songs.git
cd songs
mvn package
```

Check everything is installed:

```bash
./bin/songs doctor
```

If Playwright Chromium is missing, install it with:

```bash
./bin/songs setup-browser
```

Optional: make `songs` available everywhere:

```bash
mkdir -p ~/.local/bin
ln -sf "$PWD/bin/songs" ~/.local/bin/songs
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

## Usage

```bash
songs show file:///Users/you/Music
songs setup-browser
songs sync --dry-run "https://music.apple.com/..." "file:///Users/you/Music"
songs sync "https://music.apple.com/..." "file:///Users/you/Music"
```

`sync` makes the target match the source: it downloads missing tracks and **deletes** extra ones. Always run `--dry-run` first.

After editing code, rebuild:

```bash
mvn package
```
