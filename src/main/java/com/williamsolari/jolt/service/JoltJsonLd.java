package com.williamsolari.jolt.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.williamsolari.jolt.model.DocPage;

import lombok.RequiredArgsConstructor;

/**
 * schema.org structured data (JSON-LD) for the Jolt pages. The shapes live in {@code jolt/jsonld-*.json} with
 * {@code {{name}}} placeholders; this class fills them in. A string whose placeholder has no value (the version,
 * until GitHub has answered) is left out entirely instead of being printed as "null".
 *
 * <p>The injected Gson keeps its default HTML-safe escaping (do not set {@code spring.gson.disable-html-escaping}),
 * which makes the output safe to place inside a
 * {@code <script type="application/ld+json">} element.
 */
@Component
@RequiredArgsConstructor
public class JoltJsonLd {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final JoltSite site;
    private final Gson gson;

    public String home(String description) {
        Map<String, String> values = common();
        values.put("description", description);
        values.put("version", site.isVersionKnown() ? site.getVersion() : null);
        return render("home", values);
    }

    public String doc(DocPage page) {
        Map<String, String> values = common();
        values.put("title", page.title());
        values.put("description", page.description());
        values.put("url", site.url(page.path()));
        values.put("updated", page.updated().toString());
        return render("doc", values);
    }

    public String parsel(String description) {
        Map<String, String> values = common();
        values.put("description", description);
        values.put("url", site.url("/parsel"));
        values.put("releases", site.getParselReleasesUrl());
        values.put("image", site.url("/img/inbox-dark-violet.png"));
        return render("parsel", values);
    }

    private Map<String, String> common() {
        Map<String, String> values = new HashMap<>();
        values.put("home", site.url("/"));
        values.put("docs", site.url("/docs/getting-started"));
        values.put("github", site.getGithubUrl());
        values.put("portfolio", site.getPortfolioUrl());
        return values;
    }

    private String render(String name, Map<String, String> values) {
        try (var reader = new InputStreamReader(
                new ClassPathResource("jolt/jsonld-" + name + ".json").getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement tree = JsonParser.parseReader(reader);
            fill(tree, values);
            return gson.toJson(tree);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Replaces placeholders in every string below {@code node}, removing members and items that cannot be filled. */
    private static void fill(JsonElement node, Map<String, String> values) {
        if (node instanceof JsonObject object) {
            for (Iterator<Map.Entry<String, JsonElement>> it = object.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, JsonElement> entry = it.next();
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    String text = substitute(entry.getValue().getAsString(), values);
                    if (text == null) {
                        it.remove();
                    } else {
                        entry.setValue(new JsonPrimitive(text));
                    }
                } else {
                    fill(entry.getValue(), values);
                }
            }
        } else if (node instanceof JsonArray array) {
            for (JsonElement item : array) {
                fill(item, values);
            }
        }
    }

    /** The text with its placeholders replaced, or null when one of them has no value. */
    private static String substitute(String text, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) {
                return null;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
