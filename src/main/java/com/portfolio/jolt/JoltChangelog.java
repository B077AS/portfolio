package com.portfolio.jolt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The release history shown on the changelog page. It is read from the CHANGELOG.md in the Jolt repository on GitHub
 * (so a release only needs the file updated), parsed, and cached. The cache is refreshed in the background at most
 * once an hour; if GitHub cannot be reached the last good copy stays.
 */
@Component
public class JoltChangelog {

    private static final Logger log = LoggerFactory.getLogger(JoltChangelog.class);
    private static final Duration REFRESH_EVERY = Duration.ofHours(1);
    private static final Duration RETRY_EVERY = Duration.ofMinutes(2);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final Pattern RELEASE = Pattern.compile("^##\\s+v?(\\d+\\.\\d+\\.\\d+)(?:\\s*[-–]\\s*(\\d{4}-\\d{2}-\\d{2}))?\\s*$");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\((https://[^\\s)]+)\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");

    /** One bullet: its text as safe HTML, and the indented bullets under it. */
    public record Item(String html, List<String> subs) {
    }

    /** A group of bullets under a heading such as "Added". The title is empty for plain paragraphs. */
    public record Section(String title, List<Item> items) {
        /** Lowercase title used as a CSS class. */
        public String kind() {
            return title.toLowerCase().replaceAll("[^a-z]", "");
        }
    }

    public record Release(String version, String date, List<Section> sections) {
        public String anchor() {
            return "v" + version;
        }
    }

    private final String url;
    private volatile List<Release> releases = List.of();
    private volatile Instant lastAttempt = Instant.EPOCH;
    private volatile Instant lastSuccess = Instant.EPOCH;
    private final AtomicBoolean refreshing = new AtomicBoolean();

    public JoltChangelog(@Value("${site.jolt.changelog-url:}") String url) {
        this.url = url;
        releases(); // start the first download right away
    }

    /** The parsed releases, newest first. Empty only if GitHub has never answered. */
    public List<Release> releases() {
        if (!url.isBlank()) {
            Duration age = Duration.between(lastSuccess, Instant.now());
            Duration sinceTry = Duration.between(lastAttempt, Instant.now());
            boolean due = releases.isEmpty() ? sinceTry.compareTo(RETRY_EVERY) > 0 : age.compareTo(REFRESH_EVERY) > 0 && sinceTry.compareTo(RETRY_EVERY) > 0;
            if (due && refreshing.compareAndSet(false, true)) {
                lastAttempt = Instant.now();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "williamsolari.com")
                        .timeout(Duration.ofSeconds(8))
                        .build();
                var future = HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(this::apply)
                        .whenComplete((ok, error) -> {
                            if (error != null) {
                                log.warn("Could not read the Jolt changelog from {}: {}", url, error.toString());
                            }
                            refreshing.set(false);
                        });
                if (releases.isEmpty()) {
                    // nothing to show yet: wait briefly for the first copy instead of rendering an empty page
                    try {
                        future.get(9, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                        // logged above, or still running; the page falls back to a link to GitHub
                    }
                }
            }
        }
        return releases;
    }

    private void apply(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            log.warn("GitHub answered {} for the Jolt changelog", response.statusCode());
            return;
        }
        List<Release> parsed = parse(response.body());
        if (!parsed.isEmpty()) {
            releases = parsed;
            lastSuccess = Instant.now();
        }
    }

    /** Parses the Keep-a-Changelog style file. Sections that only list what has not been checked are left out. */
    static List<Release> parse(String markdown) {
        List<Release> out = new ArrayList<>();
        String version = null, date = null, title = "";
        List<Section> sections = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        StringBuilder text = null;
        List<String> subs = new ArrayList<>();
        StringBuilder sub = null;
        boolean skip = false;

        for (String raw : (markdown + "\n## end 0.0.0\n").split("\\R")) {
            String line = raw.stripTrailing();
            boolean release = line.startsWith("## ");
            boolean heading = line.startsWith("### ");
            if (release || heading) {
                flush(items, text, subs, sub, skip);
                text = null; sub = null; subs = new ArrayList<>();
                if (version != null && !items.isEmpty() && !skip) sections.add(new Section(title, items));
                items = new ArrayList<>();
                if (release) {
                    if (version != null && !sections.isEmpty()) out.add(new Release(version, date, sections));
                    sections = new ArrayList<>();
                    Matcher m = RELEASE.matcher(line);
                    version = m.matches() ? m.group(1) : null;
                    date = m.matches() ? m.group(2) : null;
                    title = "";
                    skip = false;
                } else {
                    title = line.substring(4).trim();
                    skip = title.toLowerCase().startsWith("not verified");
                }
                continue;
            }
            if (version == null || line.isBlank()) {
                if (line.isBlank() && text != null && version != null) {
                    // a blank line ends a plain paragraph, but a bullet may continue after it only if indented
                    if (title.isEmpty()) {
                        flush(items, text, subs, sub, skip);
                        text = null; sub = null; subs = new ArrayList<>();
                    }
                }
                continue;
            }
            if (line.startsWith("- ")) {
                flush(items, text, subs, sub, skip);
                text = new StringBuilder(line.substring(2)); sub = null; subs = new ArrayList<>();
            } else if (line.matches("^\\s+[-*] .*")) {
                if (sub != null) subs.add(sub.toString());
                sub = new StringBuilder(line.trim().substring(2));
            } else if (line.startsWith(" ") && sub != null) {
                sub.append(' ').append(line.trim());
            } else if (line.startsWith(" ") && text != null) {
                text.append(' ').append(line.trim());
            } else if (text != null && title.isEmpty()) {
                text.append(' ').append(line.trim());
            } else if (text == null) {
                text = new StringBuilder(line.trim()); // plain paragraph under a release
            }
        }
        return out;
    }

    private static void flush(List<Item> items, StringBuilder text, List<String> subs, StringBuilder sub, boolean skip) {
        if (text == null || skip) return;
        List<String> all = new ArrayList<>(subs);
        if (sub != null) all.add(sub.toString());
        items.add(new Item(inline(cutUnverified(text.toString())), all.stream().map(s -> inline(cutUnverified(s))).toList()));
    }

    /** Drops a trailing "Not verified: ..." remark; the site only shows what is confirmed. */
    private static String cutUnverified(String s) {
        int i = s.indexOf("Not verified");
        return i < 0 ? s : s.substring(0, i).stripTrailing();
    }

    /** Escapes the text and turns `code`, **bold** and [links](https://...) into HTML. Nothing else is allowed through. */
    static String inline(String s) {
        // code spans first, so that **bold** around them still works; they are put back after the other rules ran
        List<String> codes = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        String[] parts = s.split("`", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 1 && i < parts.length - 1) {
                text.append('').append(codes.size()).append('');
                codes.add("<code>" + escape(parts[i]) + "</code>");
            } else {
                text.append(i % 2 == 1 ? "`" : "").append(parts[i]); // an unterminated backtick stays plain text
            }
        }
        String t = escape(text.toString());
        t = LINK.matcher(t).replaceAll(m -> "<a href=\"" + Matcher.quoteReplacement(m.group(2)) + "\" rel=\"noopener\">" + Matcher.quoteReplacement(m.group(1)) + "</a>");
        t = BOLD.matcher(t).replaceAll("<strong>$1</strong>");
        for (int i = 0; i < codes.size(); i++) {
            t = t.replace("" + i + "", codes.get(i));
        }
        return t;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
