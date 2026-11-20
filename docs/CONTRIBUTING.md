# Contributing

## Build and tests

Java 11+ and Maven. Everything runs locally; there is no CI.

| Command | What it does | Network |
|---|---|---|
| `mvn test` | offline tests: unit tests, the whole pipeline on recorded YouTube answers, downloads against a local HTTP server | no |
| `mvn test -Plive` | live checks against YouTube: every client profile, a corpus of videos, downloads, playlists, search, subtitles; summary in `target/live-report.md` | yes |
| `mvn test -Plive -Dyt.cookies=<cookies.txt>` | adds the logged-in checks (age-restricted video) | yes |
| `mvn test -Precord` | re-records the fixtures in `src/test/resources/fixtures` | yes |

Tests that need Node.js 22+ (the JS challenge solver) are skipped when it is not installed.

Live checks have slack in their expectations (at least, not exactly) so normal YouTube changes do not turn them red.
Known limitations are reported as skipped with a reason. When a reference video disappears, the failure says so:
replace its id in `src/test/java/.../testing/Corpus.java`.

## When YouTube changes something

1. `mvn test -Plive` turns red, or a user reports a problem (ask for `attempts()` and `warnings()`).
2. `mvn test -Precord` to record what YouTube sends now. `git diff --stat src/test/resources/fixtures` shows which
   answers changed.
3. `mvn test`: an offline test now fails the same way. Fix it there, without the network.
4. `mvn test -Plive` again, then commit code and fixtures together.

Typical fixes:

- **Bot check or HTTP 400 for a client profile**: its version or device fields are stale. Compare with
  `INNERTUBE_CLIENTS` in [yt-dlp](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/youtube/_base.py)
  and update `ClientProfiles`. yt-dlp's `_DEFAULT_CLIENTS` / `_DEFAULT_JSLESS_CLIENTS` show which clients work now.
- **A list page changed shape** (playlists, search): parsing goes by renderer name in
  `internal/youtube/PlaylistExtractor` and `SearchExtractor`; add the new renderer next to the old one.
- **JS challenges fail** ("whose JS challenges were not solved"): update the EJS scripts, see below.

## Updating the EJS scripts

`src/main/resources/com/github/kiulian/downloader/js/` holds `yt.solver.core.min.js` and `yt.solver.lib.min.js`
from a [yt-dlp/ejs release](https://github.com/yt-dlp/ejs/releases). To update:

1. Download both files of the release yt-dlp currently pins (`yt_dlp/extractor/youtube/jsc/_builtin/vendor/_info.py`).
2. Check them against the SHA3-512 hashes in that `_info.py`.
3. Replace the files, set `EjsSolver.VERSION` and the two hashes in `EjsSolver`.
4. `mvn test -Precord`, then `mvn test` (needs Node.js 22+).

## Fixtures and secrets

- The recorder masks the caller's IP address in recorded answers; check `git diff` before committing anyway.
- Pages are stored as their `ytInitialData` only; one player script (about 1 MB) is kept.
- Account cookies are secrets. `*cookies*.txt` is git-ignored; never commit cookies or paste them into issues.

## Code

- No new runtime dependencies. JSON is parsed by `internal/json`.
- Public API: `YoutubeDownloader`, `YoutubeException`, `model/`, `download/`, `client/`, `http/`, `js/`, `auth/`.
  Everything under `internal/` may change at any time.
- New parsing logic comes with a test on a recorded or hand-written answer.
