# Jobs feature — implementation plan

## Motivation

Today `songs sync <src> <tgt>` re-passes the same URIs on every run. We want to
**name** and **save** a source/target pair as a job, then execute it by name:

```
songs jobs create favourite <src-uri> <tgt-uri>
songs jobs run favourite
```

At the same time the CLI's `SyncCommand` has become heavy: it wires the
repository registry, resolves reader/writer from URIs, constructs a use case,
runs it, and renders — three responsibilities in one class. Introducing a
`SyncJob` abstraction in core lets the CLI shrink to a thin adapter.

## Design decisions (locked in with the user)

1. **Storage**: single JSON config file at `~/.songs/config.json` (override
   with `SONGS_CONFIG=/path/to/file.json`). Top-level keys `settings` and
   `jobs` keep global CLI defaults and named jobs cleanly separated in one
   file. Jackson (already a dep) handles read/write.
2. **`SyncPlaylistUseCase` is deleted** — replaced by `SyncJobRunner`.
3. **`songs sync` is folded into `songs jobs run`**. Ad-hoc invocations pass
   `--source` and `--target`; saved-job invocations pass a name.

## Config file format

```json
{
  "settings": {
    "concurrency": 4
  },
  "jobs": {
    "favourite": {
      "source": "https://music.apple.com/pl/playlist/favourite/pl.u-KVXBDYPFd29j5g",
      "target": "/Users/me/Music/Favourite"
    },
    "workout": {
      "source": "https://music.apple.com/pl/playlist/workout/pl.u-abc123",
      "target": "/Users/me/Music/Workout"
    }
  }
}
```

- Both top-level keys are optional; missing file → empty config; missing
  `settings` → all defaults; missing `jobs` → no saved jobs.
- Job names are the keys under `jobs`. Names must be non-blank and contain no
  `/` or whitespace.
- Unknown keys inside a job object are preserved on rewrite (forward-compat).
- Unknown keys inside `settings` are preserved but ignored by v1.
- Jackson (already a dependency) does (de)serialization; no new libraries.

## Package layout

### `songs-core`

```
com.songs.config
    SongsSettings               record: concurrency (Optional<Integer>) + raw Map for unknown keys
    SongsConfig                 record: SongsSettings settings + Map<String, SyncJob> jobs
    SongsConfigFile             load/save from ~/.songs/config.json (or SONGS_CONFIG)

com.songs.jobs
    SyncJob                     record(name, sourceUri, targetUri) + toMap/fromMap + validation
    JobStore                    port: save / load / delete / list
    ConfigFileJobStore          adapter over SongsConfigFile (mutates jobs map, saves)
    SyncJobRunner               preview(job) / run(job, dryRun, concurrency)
```

### `songs-cli`

```
com.songs.cli
    SongsCommand                add JobsCommand; drop SyncCommand
    JobsCommand                 parent for the subcommands below
    JobsCreateCommand
    JobsListCommand
    JobsGetCommand
    JobsDeleteCommand
    JobsRunCommand              positional [name] OR --source + --target
    RepositoryFactory           unchanged (still builds the registry)
    Renderer                    unchanged
```

## Core types

### `SyncJob`

```java
public record SyncJob(String name, String sourceUri, String targetUri) {
    public SyncJob {
        requireNonBlank(name, "name");
        if (name.contains("/") || name.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Invalid job name: " + name);
        }
        requireNonBlank(sourceUri, "sourceUri");
        requireNonBlank(targetUri, "targetUri");
    }

    public Map<String, String> toMap() {
        return Map.of("source", sourceUri, "target", targetUri);
    }

    public static SyncJob fromMap(String name, Map<String, String> map) {
        String source = map.get("source");
        String target = map.get("target");
        if (source == null || target == null) {
            throw new IllegalArgumentException("Job [" + name + "] missing source/target");
        }
        return new SyncJob(name, source, target);
    }
}
```

### `JobStore`

```java
public interface JobStore {
    void save(SyncJob job);
    Optional<SyncJob> load(String name);
    boolean delete(String name);
    List<SyncJob> list();
}
```

### `SyncJobRunner`

Absorbs today's `SyncPlaylistUseCase`. Owns the registry so URI-dispatch stops
leaking into the CLI:

```java
public final class SyncJobRunner {
    private final PlaylistRepositoryRegistry registry;
    private final PlaylistSynchronizer synchronizer;

    public SyncJobRunner(PlaylistRepositoryRegistry registry, PlaylistSynchronizer synchronizer) { ... }

    public SyncPlan preview(SyncJob job) throws IOException {
        return synchronizer.plan(
            registry.readerFor(job.sourceUri()).extract(job.sourceUri()),
            registry.readerFor(job.targetUri()).extract(job.targetUri())
        );
    }

    public SyncResult run(SyncJob job) throws IOException {
        SyncPlan plan = preview(job);
        return synchronizer.apply(plan, registry.writerFor(job.targetUri()));
    }
}
```

### `SongsConfigFile`

- `SongsConfig load()` — parse; missing file → empty config (`new SongsConfig(SongsSettings.empty(), Map.of())`).
- `void save(SongsConfig)` — write pretty-printed JSON atomically (`config.json.tmp` + rename).
- Resolves path from `SONGS_CONFIG` env var, else `~/.songs/config.json`. Creates the parent directory (`~/.songs`) with mode `0700` on first save.
- Preserves unknown keys inside job objects and inside `settings` when saving (round-trip stable).

### `ConfigFileJobStore`

Thin adapter: `save/load/delete/list` translate to `SongsConfig` mutations then
`SongsConfigFile.save`. Settings are untouched.

## CLI commands

### `songs jobs create <name> <source-uri> <target-uri> [--force]`
- Validates URIs against registry (`registry.readerFor(...)` succeeds for source
  and `registry.writerFor(...)` succeeds for target).
- Fails if `<name>` exists unless `--force`.
- Exit 0 on success, 2 on validation error, 3 on conflict.

### `songs jobs list`
- Prints one line per job: `name\tsource\ttarget`.
- Exit 0.

### `songs jobs get <name>`
- Prints a key/value listing (`name`, `source`, `target`).
- Exit 0 on success, 4 if not found.

### `songs jobs delete <name>`
- Removes the entry from `jobs`. Exit 0 on success, 4 if not found.

### `songs jobs run [name] [--source URI] [--target URI] [--dry-run] [--concurrency N]`
- If `name` is given: load the job from the store.
- Else if both `--source` and `--target` are given: build a transient job with
  name `"(inline)"`.
- Else: error, print usage. Exit 2.
- `--concurrency` defaults to `settings.concurrency` if set, else `4`.
- Delegates to `SyncJobRunner.preview(...)` or `.run(...)`, then `Renderer`.

### Removed
- `SyncCommand` — deleted; behaviour lives in `JobsRunCommand` (`--source/--target`).
- `SyncPlaylistUseCase` — deleted.

## Migration / breaking changes

- `songs sync <src> <tgt>` → `songs jobs run --source <src> --target <tgt>`.
  Bump version to **0.2.0** because this is a breaking CLI change. Document in
  the release notes.
- No file-format migration needed — feature is new.

## Testing

New tests:
- `SyncJobTest` — validation, `toMap`/`fromMap` round-trip.
- `SongsConfigFileTest` — round-trip through a `@TempDir` file; `SONGS_CONFIG` override; missing file yields empty config; unknown keys in `settings` and inside a job are preserved after save.
- `ConfigFileJobStoreTest` — save/load/delete/list against a `@TempDir` config; verifies `settings` is untouched by job mutations.
- `SyncJobRunnerTest` — preview + run with fake registry, mirroring the deleted `SyncPlaylistUseCase` tests.

CLI tests:
- `JobsCreateCommandTest`, `JobsRunCommandTest` (with `--source/--target` and with a saved job name), `JobsDeleteCommandTest`, `JobsGetCommandTest`, `JobsListCommandTest`.

Delete: `SyncPlaylistUseCaseTest` (superseded).

## Rollout

1. Land core (`config`, `jobs` packages + tests).
2. Land CLI (JobsCommand tree, delete `SyncCommand`).
3. Update `SongsCommand`'s `subcommands` and `version = "songs 0.2.0"`.
4. Update `README` / help text.
5. Release `v0.2.0`; update the Homebrew formula.

## Open questions to confirm before coding

- **`settings` keys** to support in v1 — only `concurrency`, or also `browser_cache`, `log_level`? (Recommended: just `concurrency`; extend later. Unknown keys are preserved either way.)
- **`jobs get`** output — key/value listing or a JSON dump of the job object? (Recommended: key/value; raw JSON is available via `jq '.jobs.favourite' ~/.songs/config.json`.)
- **File permissions** — should we `chmod 600` the config on save (URIs are not secrets, but future keys might be)? (Recommended: yes, defensive default.)
