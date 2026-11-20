# Changelog

## 4.0.0-rc1

A rewrite. YouTube had changed enough that 3.x no longer worked; see [MIGRATION.md](MIGRATION.md) for the API changes.

Works again:
- Video info and formats: current client profiles (visionos, android_vr) with visitor data, up to 2160p.
- Playlists and channel uploads (new `lockupViewModel` pages), search, subtitles.

New:
- Download engine: range requests, resume after broken connections, `.part` files, progress, cancel.
- `EjsSolver`: solves YouTube's JS challenges with yt-dlp's EJS scripts in Node.js; enables the `tv_samsung`
  profile and "made for kids" videos.
- `YoutubeCookies`: logged-in requests, age-restricted videos.
- `HttpTransport`: plug in your own HTTP stack.
- Client profiles are data and can be updated without a release.
- `warnings()` and `attempts()` explain what was skipped or tried.

Changed:
- Java 11, unchecked exceptions, `CompletableFuture` instead of callbacks, immutable models.
- Dependencies: only `slf4j-api` (fastjson and logback removed).

Removed:
- The regex signature decipherer (replaced by `EjsSolver`), `Config`, the `Request*`/`Response` types, `ClientType`,
  `Base64Encoder`, the old subtitles list endpoint.

Known limitations:
- Machine-translated subtitles get HTTP 429 from YouTube.
- Web client formats are SABR-only and not supported.
