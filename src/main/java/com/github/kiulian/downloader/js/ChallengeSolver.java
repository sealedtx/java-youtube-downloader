package com.github.kiulian.downloader.js;

import java.io.IOException;
import java.util.Set;

/**
 * Solves the JS challenges of YouTube's player: the {@code n} parameter of stream URLs and the signature of
 * ciphered formats. Needed only for client profiles with {@code requiresJsPlayer()}.
 * <p>
 * The library ships {@link EjsSolver}, which runs yt-dlp's challenge scripts in Node.js (or another JS runtime).
 * Implement this interface to use something else, for example a WebView on Android.
 */
public interface ChallengeSolver {

    /**
     * @param playerId   id of the player version, for example {@code 5ead9320}; the same id always has the same code,
     *                   so work may be cached per id
     * @param playerCode the player JavaScript ({@code base.js})
     * @param n          values of the {@code n} parameter to transform
     * @param signatures ciphered signatures ({@code s} of {@code signatureCipher}) to decipher
     * @return the solutions; a challenge missing from the result counts as unsolved and its format is skipped
     */
    ChallengeSolutions solve(String playerId, String playerCode, Set<String> n, Set<String> signatures) throws IOException;
}
