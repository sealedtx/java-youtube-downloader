package com.github.kiulian.downloader.js;

import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ChallengeSolver} that runs yt-dlp's EJS scripts (vendored, version {@value #VERSION}) in an external
 * JavaScript runtime. JVM only; on Android implement {@link ChallengeSolver} yourself.
 * <p>
 * The player code is YouTube's JavaScript, so it runs in a separate process with as few rights as the runtime
 * allows: {@link #node()} uses Node's permission model (no file system, no child processes). Each call starts one
 * process for all challenges of a video; the preprocessed player that EJS returns is kept for later calls.
 */
public final class EjsSolver implements ChallengeSolver {

    public static final String VERSION = "0.8.0";

    // SHA3-512 of the vendored files, the same values yt-dlp pins for EJS 0.8.0
    private static final String CORE_HASH = "ee5b307d07f55e91e4723edf5ac205cc877a474187849d757dc1322e38427b157a9d706d510c1723d3670f98e5a3f8cbcde77874a80406bd7204bc9fea30f283";
    private static final String LIB_HASH = "8420c259ad16e99ce004e4651ac1bcabb53b4457bf5668a97a9359be9a998a789fee8ab124ee17f91a2ea8fd84e0f2b2fc8eabcaf0b16a186ba734cf422ad053";
    private static final int MAX_OUTPUT_BYTES = 64 * 1024 * 1024;
    private static final int CACHED_PLAYERS = 4;

    private static volatile String scripts;

    private final List<String> command;
    private final long timeoutMillis;
    private final Map<String, String> preprocessedPlayers = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > CACHED_PLAYERS;
                }
            });

    private EjsSolver(List<String> command, long timeoutMillis) {
        this.command = Collections.unmodifiableList(new ArrayList<>(command));
        this.timeoutMillis = timeoutMillis;
    }

    /** Node.js 22 or newer from the {@code PATH}, with the permission model on. */
    public static EjsSolver node() {
        return node("node");
    }

    /** Node.js 22 or newer at this path, with the permission model on. */
    public static EjsSolver node(String executable) {
        return new EjsSolver(Arrays.asList(Objects.requireNonNull(executable, "executable"), "--permission", "-"), 60_000);
    }

    /**
     * Any runtime that runs a script from stdin and prints with {@code console.log}, for example
     * {@code deno run --no-remote --no-npm --no-config -}. Sandboxing is up to the command.
     */
    public static EjsSolver command(List<String> command) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command is empty");
        }
        return new EjsSolver(command, 60_000);
    }

    /** {@link #node()} if a Node.js 22+ is on the {@code PATH}, otherwise {@code null}. Starts {@code node --version}. */
    public static EjsSolver detect() {
        try {
            Process process = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
            String version = new String(readAll(process.getInputStream(), 1024), StandardCharsets.UTF_8).trim();
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return null;
            }
            int major = Integer.parseInt(version.replaceFirst("^v", "").split("\\.")[0]);
            return major >= 22 ? node() : null;
        } catch (IOException | NumberFormatException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public ChallengeSolutions solve(String playerId, String playerCode, Set<String> n, Set<String> signatures) throws IOException {
        List<String> nList = new ArrayList<>(n);
        List<String> sigList = new ArrayList<>(signatures);
        List<Map<String, Object>> requests = new ArrayList<>();
        if (!nList.isEmpty()) {
            requests.add(request("n", nList));
        }
        if (!sigList.isEmpty()) {
            requests.add(request("sig", sigList));
        }
        if (requests.isEmpty()) {
            return new ChallengeSolutions(Collections.emptyMap(), Collections.emptyMap());
        }

        Map<String, Object> input = new LinkedHashMap<>();
        String preprocessed = preprocessedPlayers.get(playerId);
        if (preprocessed != null) {
            input.put("type", "preprocessed");
            input.put("preprocessed_player", preprocessed);
        } else {
            input.put("type", "player");
            input.put("player", playerCode);
            input.put("output_preprocessed", true);
        }
        input.put("requests", requests);

        JsonNode output = Json.parse(run(scripts() + "\nconsole.log(JSON.stringify(jsc(" + Json.write(input) + ")));\n"));
        if (!"result".equals(output.get("type").asString())) {
            throw new IOException("Challenge solver failed: " + output.get("error").asString("unknown error"));
        }
        String newPreprocessed = output.get("preprocessed_player").asString();
        if (newPreprocessed != null) {
            preprocessedPlayers.put(playerId, newPreprocessed);
        }

        Map<String, String> nSolutions = new HashMap<>();
        Map<String, String> sigSolutions = new HashMap<>();
        List<JsonNode> responses = output.get("responses").items();
        for (int i = 0; i < requests.size() && i < responses.size(); i++) {
            JsonNode response = responses.get(i);
            if (!"result".equals(response.get("type").asString())) {
                continue; // unsolved: the formats that need it are skipped
            }
            Map<String, String> target = "n".equals(requests.get(i).get("type")) ? nSolutions : sigSolutions;
            JsonNode data = response.get("data");
            for (String challenge : data.keys()) {
                String solution = data.get(challenge).asString();
                if (solution != null) {
                    target.put(challenge, solution);
                }
            }
        }
        return new ChallengeSolutions(nSolutions, sigSolutions);
    }

    private static Map<String, Object> request(String type, List<String> challenges) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("type", type);
        request.put("challenges", challenges);
        return request;
    }

    private String run(String script) throws IOException {
        Process process = new ProcessBuilder(command).start();
        // blocking reads get their own threads, not the shared ForkJoin pool
        Executor readers = task -> {
            Thread thread = new Thread(task, "ejs-solver-output");
            thread.setDaemon(true);
            thread.start();
        };
        CompletableFuture<byte[]> stdout = CompletableFuture.supplyAsync(() -> readQuietly(process.getInputStream(), MAX_OUTPUT_BYTES), readers);
        CompletableFuture<byte[]> stderr = CompletableFuture.supplyAsync(() -> readQuietly(process.getErrorStream(), 64 * 1024), readers);
        try (OutputStream in = process.getOutputStream()) {
            in.write(script.getBytes(StandardCharsets.UTF_8));
        }
        try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                throw new IOException("Challenge solver timed out after " + timeoutMillis + " ms: " + String.join(" ", command));
            }
            byte[] out = stdout.get(5, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                String error = new String(stderr.get(5, TimeUnit.SECONDS), StandardCharsets.UTF_8).trim();
                throw new IOException("Challenge solver exited with " + process.exitValue() + ": "
                        + (error.length() > 500 ? error.substring(0, 500) + "..." : error));
            }
            return new String(out, StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while solving challenges", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException("Could not read the challenge solver output", e);
        } finally {
            process.destroyForcibly();
        }
    }

    // lib defines `lib` (meriyah, astring), core defines `jsc` and expects them as globals
    private static String scripts() throws IOException {
        String loaded = scripts;
        if (loaded == null) {
            String lib = resource("yt.solver.lib.min.js", LIB_HASH);
            String core = resource("yt.solver.core.min.js", CORE_HASH);
            loaded = lib + "\nObject.assign(globalThis, lib);\n" + core;
            scripts = loaded;
        }
        return loaded;
    }

    private static String resource(String name, String expectedHash) throws IOException {
        try (InputStream in = EjsSolver.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Missing challenge solver script " + name);
            }
            byte[] bytes = readAll(in, 4 * 1024 * 1024);
            String hash = hex(MessageDigest.getInstance("SHA3-512").digest(bytes));
            if (!hash.equals(expectedHash)) {
                throw new IOException("Challenge solver script " + name + " does not match its pinned hash");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA3-512 is not available", e);
        }
    }

    private static byte[] readQuietly(InputStream in, int maxBytes) {
        try {
            return readAll(in, maxBytes);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static byte[] readAll(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() + n > maxBytes) {
                throw new IOException("Output larger than " + maxBytes + " bytes");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
