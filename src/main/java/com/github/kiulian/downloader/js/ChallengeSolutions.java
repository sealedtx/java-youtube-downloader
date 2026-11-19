package com.github.kiulian.downloader.js;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Answers of a {@link ChallengeSolver}: challenge to solution, for {@code n} values and for signatures.
 */
public final class ChallengeSolutions {

    private final Map<String, String> n;
    private final Map<String, String> signatures;

    public ChallengeSolutions(Map<String, String> n, Map<String, String> signatures) {
        this.n = Collections.unmodifiableMap(new HashMap<>(n));
        this.signatures = Collections.unmodifiableMap(new HashMap<>(signatures));
    }

    /** Transformed {@code n}, or {@code null} if unsolved. */
    public String n(String challenge) {
        return n.get(challenge);
    }

    /** Deciphered signature, or {@code null} if unsolved. */
    public String signature(String challenge) {
        return signatures.get(challenge);
    }

    @Override
    public String toString() {
        return "ChallengeSolutions{" + n.size() + " n, " + signatures.size() + " signatures}";
    }
}
