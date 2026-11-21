package com.williamsolari.jolt.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

/**
 * The latest released version of a GitHub project, from the tag of its latest release. A refresh runs in the background at
 * most once an hour (every two minutes while no version is known yet) and the current value is returned. Only while GitHub
 * has never answered does {@link #version()} wait, briefly, for the first lookup.
 */
final class LatestRelease {

    private static final Logger log = LoggerFactory.getLogger(LatestRelease.class);
    private static final Duration REFRESH_EVERY = Duration.ofHours(1);
    private static final Duration RETRY_EVERY = Duration.ofMinutes(2);
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+\\.\\d+\\.\\d+)$");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final String name;
    private final String url;
    private volatile String version;
    private volatile CompletableFuture<Void> lookup;
    private volatile Instant lastCheck = Instant.EPOCH;
    private final AtomicBoolean refreshing = new AtomicBoolean();

    /**
     * @param name what is being looked up, for log messages
     * @param url  GitHub's "latest release" API address; blank turns the lookup off
     */
    LatestRelease(String name, String url) {
        this.name = name;
        this.url = url;
        refresh(); // start the first lookup right away
    }

    /** The version such as {@code 0.5.0}, or null if GitHub has not answered (yet). */
    String version() {
        refresh();
        if (version == null && lookup != null) {
            try {
                lookup.get(9, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // logged by the lookup itself
            }
        }
        return version;
    }

    /** Whether GitHub has told us the version yet, without waiting for it. */
    boolean isKnown() {
        refresh();
        return version != null;
    }

    private void refresh() {
        Duration every = version == null ? RETRY_EVERY : REFRESH_EVERY;
        if (!url.isBlank()
                && Duration.between(lastCheck, Instant.now()).compareTo(every) > 0
                && refreshing.compareAndSet(false, true)) {
            lastCheck = Instant.now(); // also after a failure, so an unreachable GitHub is retried later, not per request
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "williamsolari.com")
                    .timeout(Duration.ofSeconds(8))
                    .build();
            lookup = HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::apply)
                    .whenComplete((ok, error) -> {
                        if (error != null) {
                            log.warn("Could not read the latest {} version from {}: {}", name, url, error.toString());
                        }
                        refreshing.set(false);
                    });
        }
    }

    private void apply(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            log.warn("GitHub answered {} for the latest {} release", response.statusCode(), name);
            return;
        }
        String tag = JsonParser.parseString(response.body()).getAsJsonObject().get("tag_name").getAsString();
        var matcher = VERSION.matcher(tag);
        if (matcher.matches()) {
            version = matcher.group(1);
        } else {
            log.warn("Ignoring unexpected {} release tag '{}'", name, tag);
        }
    }
}
