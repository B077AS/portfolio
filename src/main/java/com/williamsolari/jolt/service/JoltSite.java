package com.williamsolari.jolt.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.williamsolari.core.web.SiteHost;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Settings of the Jolt showcase site, which is served by this application on its own host
 * (for example {@code jolt.williamsolari.com}) next to the portfolio.
 */
@Component
@Getter
public class JoltSite implements SiteHost {

    /** Request host names (no port) that belong to the Jolt site. */
    private final List<String> hosts;
    /** Public origin used for canonical URLs, Open Graph tags and the sitemap. No trailing slash. */
    private final String baseUrl;
    private final String githubUrl;
    private final String parselGithubUrl;
    private final String parselReleasesUrl;
    private final String mavenCentralUrl;
    private final String jpackGithubUrl;
    private final String jpackMavenCentralUrl;
    /** The main website (the portfolio), linked from the Jolt pages and used as the author URL in structured data. */
    private final String portfolioUrl;

    /** Latest released Jolt version, from GitHub's latest release tag. */
    @Getter(AccessLevel.NONE)
    private final LatestRelease joltRelease;
    /** Latest released jpack version, the same way. */
    @Getter(AccessLevel.NONE)
    private final LatestRelease jpackRelease;

    public JoltSite(@Value("${site.jolt.hosts}") List<String> hosts,
                    @Value("${site.jolt.base-url}") String baseUrl,
                    @Value("${site.jolt.version-url:}") String versionUrl,
                    @Value("${site.jolt.github-url}") String githubUrl,
                    @Value("${site.jolt.parsel-github-url}") String parselGithubUrl,
                    @Value("${site.jolt.parsel-releases-url}") String parselReleasesUrl,
                    @Value("${site.jolt.maven-central-url}") String mavenCentralUrl,
                    @Value("${site.jolt.jpack-version-url:}") String jpackVersionUrl,
                    @Value("${site.jolt.jpack-github-url}") String jpackGithubUrl,
                    @Value("${site.jolt.jpack-maven-central-url}") String jpackMavenCentralUrl,
                    @Value("${site.portfolio-url}") String portfolioUrl) {
        this.hosts = hosts.stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.githubUrl = githubUrl;
        this.parselGithubUrl = parselGithubUrl;
        this.parselReleasesUrl = parselReleasesUrl;
        this.mavenCentralUrl = mavenCentralUrl;
        this.jpackGithubUrl = jpackGithubUrl;
        this.jpackMavenCentralUrl = jpackMavenCentralUrl;
        this.portfolioUrl = portfolioUrl;
        this.joltRelease = new LatestRelease("Jolt", versionUrl);
        this.jpackRelease = new LatestRelease("jpack", jpackVersionUrl);
    }

    /**
     * The latest released Jolt version, read from GitHub. A refresh runs in the background at most once an hour. Only while
     * GitHub has never answered does this wait, briefly, for the first lookup; if that fails it returns "latest" (see
     * {@link #isVersionKnown()}).
     */
    public String getVersion() {
        String version = joltRelease.version();
        return version == null ? "latest" : version;
    }

    /** Whether GitHub has told us the version yet; templates hide the version badge until it has. */
    public boolean isVersionKnown() {
        return joltRelease.isKnown();
    }

    /** The latest released jpack version, like {@link #getVersion()}: "latest" until GitHub has answered. */
    public String getJpackVersion() {
        String version = jpackRelease.version();
        return version == null ? "latest" : version;
    }

    public boolean isJpackVersionKnown() {
        return jpackRelease.isKnown();
    }

    @Override
    public String pathPrefix() {
        return "/jolt";
    }

    @Override
    public boolean ownsHost(String host) {
        return host != null && hosts.contains(host.toLowerCase(Locale.ROOT));
    }

    /** Absolute URL for a path on the Jolt site, e.g. {@code url("/docs")}. */
    public String url(String path) {
        return "/".equals(path) ? baseUrl + "/" : baseUrl + path;
    }
}
