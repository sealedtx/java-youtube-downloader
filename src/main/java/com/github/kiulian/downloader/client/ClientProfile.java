package com.github.kiulian.downloader.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One YouTube app identity used for innertube requests: name, version, user agent and device fields.
 * <p>
 * YouTube treats each app differently and blocks old versions, so these values go stale. When that happens you
 * can fix it without waiting for a release: {@code ClientProfiles.VISIONOS.toBuilder().version("1.03").build()},
 * and pass your list to {@code YoutubeDownloader.builder().clients(...)}.
 */
public final class ClientProfile {

    private final String name;
    private final String clientName;
    private final int clientId;
    private final String version;
    private final String userAgent;
    private final Map<String, Object> context;
    private final boolean requiresJsPlayer;

    private ClientProfile(Builder builder) {
        this.name = builder.name;
        this.clientName = builder.clientName;
        this.clientId = builder.clientId;
        this.version = builder.version;
        this.userAgent = builder.userAgent;
        this.context = Collections.unmodifiableMap(new LinkedHashMap<>(builder.context));
        this.requiresJsPlayer = builder.requiresJsPlayer;
    }

    /**
     * @param name       short name used in messages, for example {@code visionos}
     * @param clientName innertube client name, for example {@code VISIONOS}
     * @param clientId   numeric client id sent as {@code X-YouTube-Client-Name}, for example 101
     */
    public static Builder builder(String name, String clientName, int clientId) {
        return new Builder(name, clientName, clientId);
    }

    public String name() {
        return name;
    }

    /** Innertube {@code clientName}. */
    public String clientName() {
        return clientName;
    }

    /** Numeric id sent as {@code X-YouTube-Client-Name}. */
    public int clientId() {
        return clientId;
    }

    public String version() {
        return version;
    }

    public String userAgent() {
        return userAgent;
    }

    /** Extra fields of the innertube {@code context.client} object, such as {@code deviceModel}. */
    public Map<String, Object> context() {
        return context;
    }

    /**
     * True if this client's stream URLs need the JS challenges of YouTube's player solved (and the player's
     * {@code signatureTimestamp} in the request). Such clients are used only with a {@code ChallengeSolver}.
     */
    public boolean requiresJsPlayer() {
        return requiresJsPlayer;
    }

    public Builder toBuilder() {
        Builder builder = new Builder(name, clientName, clientId).version(version).userAgent(userAgent).requiresJsPlayer(requiresJsPlayer);
        builder.context.putAll(context);
        return builder;
    }

    @Override
    public String toString() {
        return name + " (" + clientName + " " + version + ")";
    }

    public static final class Builder {
        private final String name;
        private final String clientName;
        private final int clientId;
        private String version;
        private String userAgent;
        private final Map<String, Object> context = new LinkedHashMap<>();
        private boolean requiresJsPlayer;

        private Builder(String name, String clientName, int clientId) {
            this.name = Objects.requireNonNull(name, "name");
            this.clientName = Objects.requireNonNull(clientName, "clientName");
            this.clientId = clientId;
        }

        public Builder version(String version) {
            this.version = Objects.requireNonNull(version, "version");
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
            return this;
        }

        public Builder requiresJsPlayer(boolean requiresJsPlayer) {
            this.requiresJsPlayer = requiresJsPlayer;
            return this;
        }

        /** Adds or replaces a field of {@code context.client}. Values: strings, numbers, booleans. */
        public Builder context(String key, Object value) {
            context.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        public ClientProfile build() {
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(userAgent, "userAgent");
            return new ClientProfile(this);
        }
    }
}
