package com.portfolio.jolt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.JsonParser;

import lombok.Getter;

/**
 * Settings of the Jolt showcase site, which is served by this application on its own host
 * (for example {@code jolt.williamsolari.com}) next to the portfolio.
 */
@Component
@Getter
public class JoltSite {

    private static final Logger log = LoggerFactory.getLogger(JoltSite.class);
    private static final Duration REFRESH_EVERY = Duration.ofHours(1);
    private static final Duration RETRY_EVERY = Duration.ofMinutes(2);
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+\\.\\d+\\.\\d+)$");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** Request host names (no port) that belong to the Jolt site. */
    private final List<String> hosts;
    /** Public origin used for canonical URLs, Open Graph tags and the sitemap. No trailing slash. */
    private final String baseUrl;
    private final String githubUrl = "https://github.com/B077AS/jolt";
    private final String parselGithubUrl = "https://github.com/B077AS/parsel";
    private final String parselReleasesUrl = "https://github.com/B077AS/parsel/releases/latest";
    private final String mavenCentralUrl = "https://central.sonatype.com/artifact/io.github.b077as/jolt-core";
    private final String portfolioUrl = "https://williamsolari.com";

    private final String versionUrl;
    /** Latest released version, from GitHub's latest release tag. Null until GitHub has answered once. */
    private volatile String version;
    private volatile java.util.concurrent.CompletableFuture<Void> lookup;
    private volatile Instant lastCheck = Instant.EPOCH;
    private final AtomicBoolean refreshing = new AtomicBoolean();

    public JoltSite(@Value("${site.jolt.hosts}") List<String> hosts,
                    @Value("${site.jolt.base-url}") String baseUrl,
                    @Value("${site.jolt.version-url:}") String versionUrl) {
        this.hosts = hosts.stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.versionUrl = versionUrl;
        refresh(); // start the first lookup right away
    }

    /**
     * The latest released Jolt version, read from GitHub. A refresh runs in the background at most once an hour (every
     * two minutes while no version is known yet) and the current value is returned. Only while GitHub has never
     * answered does this wait, briefly, for the first lookup; if that fails it returns "latest" (see {@link #isVersionKnown()}).
     */
    public String getVersion() {
        refresh();
        if (version == null && lookup != null) {
            try {
                lookup.get(9, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // logged by the lookup itself
            }
        }
        return version == null ? "latest" : version;
    }

    /** Whether GitHub has told us the version yet; templates hide the version badge until it has. */
    public boolean isVersionKnown() {
        refresh();
        return version != null;
    }

    private void refresh() {
        Duration every = version == null ? RETRY_EVERY : REFRESH_EVERY;
        if (!versionUrl.isBlank()
                && Duration.between(lastCheck, Instant.now()).compareTo(every) > 0
                && refreshing.compareAndSet(false, true)) {
            lastCheck = Instant.now(); // also after a failure, so an unreachable GitHub is retried later, not per request
            HttpRequest request = HttpRequest.newBuilder(URI.create(versionUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "williamsolari.com")
                    .timeout(Duration.ofSeconds(8))
                    .build();
            lookup = HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::apply)
                    .whenComplete((ok, error) -> {
                        if (error != null) {
                            log.warn("Could not read the latest Jolt version from {}: {}", versionUrl, error.toString());
                        }
                        refreshing.set(false);
                    });
        }
    }

    private void apply(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            log.warn("GitHub answered {} for the latest Jolt release", response.statusCode());
            return;
        }
        String tag = JsonParser.parseString(response.body()).getAsJsonObject().get("tag_name").getAsString();
        var matcher = VERSION.matcher(tag);
        if (matcher.matches()) {
            version = matcher.group(1);
        } else {
            log.warn("Ignoring unexpected Jolt release tag '{}'", tag);
        }
    }

    public boolean isJoltHost(String host) {
        return host != null && hosts.contains(host.toLowerCase(Locale.ROOT));
    }

    /** Absolute URL for a path on the Jolt site, e.g. {@code url("/docs")}. */
    public String url(String path) {
        return "/".equals(path) ? baseUrl + "/" : baseUrl + path;
    }
}
