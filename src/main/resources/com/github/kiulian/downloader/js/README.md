# Vendored challenge solver scripts

`yt.solver.core.min.js` and `yt.solver.lib.min.js` are the EJS scripts of yt-dlp, version 0.8.0, from
https://github.com/yt-dlp/ejs/releases/tag/0.8.0 (Unlicense; the lib file bundles meriyah 6.1.4, ISC, and
astring 1.9.0, MIT, with their license headers).

`EjsSolver` checks their SHA3-512 hashes at load time. To update: download the new release files, replace both
files, and update `EjsSolver.VERSION` and the two hashes (yt-dlp pins the same values in
`yt_dlp/extractor/youtube/jsc/_builtin/vendor/_info.py`).
