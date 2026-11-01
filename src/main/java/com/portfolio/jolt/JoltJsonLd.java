package com.portfolio.jolt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * schema.org structured data (JSON-LD) for the Jolt pages. Gson's default HTML-safe escaping makes the output safe
 * to place inside a {@code <script type="application/ld+json">} element.
 */
final class JoltJsonLd {

    private static final Gson GSON = new GsonBuilder().create();

    private JoltJsonLd() {
    }

    private static Map<String, Object> author() {
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("@type", "Person");
        author.put("name", "William Solari");
        author.put("url", "https://williamsolari.com");
        return author;
    }

    private static Map<String, Object> root() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("@context", "https://schema.org");
        return m;
    }

    static String home(JoltSite site, String description) {
        Map<String, Object> website = new LinkedHashMap<>();
        website.put("@type", "WebSite");
        website.put("name", "Jolt");
        website.put("url", site.url("/"));
        website.put("description", description);

        Map<String, Object> code = new LinkedHashMap<>();
        code.put("@type", "SoftwareSourceCode");
        code.put("name", "Jolt");
        code.put("description", description);
        code.put("url", site.url("/"));
        code.put("codeRepository", site.getGithubUrl());
        code.put("programmingLanguage", "Java");
        code.put("runtimePlatform", "Java 25");
        code.put("license", "https://www.apache.org/licenses/LICENSE-2.0");
        if (site.isVersionKnown()) code.put("version", site.getVersion());
        code.put("author", author());

        Map<String, Object> app = new LinkedHashMap<>();
        app.put("@type", "SoftwareApplication");
        app.put("name", "Jolt");
        app.put("applicationCategory", "DeveloperApplication");
        app.put("operatingSystem", "Windows, Linux");
        if (site.isVersionKnown()) app.put("softwareVersion", site.getVersion());
        app.put("url", site.url("/"));
        app.put("description", description);
        app.put("author", author());
        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("@type", "Offer");
        offer.put("price", "0");
        offer.put("priceCurrency", "USD");
        app.put("offers", offer);

        Map<String, Object> graph = root();
        graph.put("@graph", List.of(website, code, app));
        return GSON.toJson(graph);
    }

    static String doc(JoltSite site, DocPage page) {
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("@type", "TechArticle");
        article.put("headline", page.title());
        article.put("description", page.description());
        article.put("url", site.url(page.path()));
        article.put("dateModified", page.updated().toString());
        article.put("inLanguage", "en");
        article.put("author", author());
        Map<String, Object> about = new LinkedHashMap<>();
        about.put("@type", "SoftwareSourceCode");
        about.put("name", "Jolt");
        about.put("codeRepository", site.getGithubUrl());
        article.put("about", about);

        Map<String, Object> graph = root();
        graph.put("@graph", List.of(article, breadcrumbs(site,
                new String[]{"Jolt", site.url("/")},
                new String[]{"Documentation", site.url("/docs/getting-started")},
                new String[]{page.title(), site.url(page.path())}
        )));
        return GSON.toJson(graph);
    }

    static String parsel(JoltSite site, String description) {
        Map<String, Object> app = new LinkedHashMap<>();
        app.put("@type", "SoftwareApplication");
        app.put("name", "Parsel");
        app.put("applicationCategory", "CommunicationApplication");
        app.put("operatingSystem", "Windows, Linux");
        app.put("description", description);
        app.put("url", site.url("/parsel"));
        app.put("downloadUrl", site.getParselReleasesUrl());
        app.put("author", author());
        app.put("image", site.url("/img/parsel/inbox-dark-violet.png"));
        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("@type", "Offer");
        offer.put("price", "0");
        offer.put("priceCurrency", "USD");
        app.put("offers", offer);

        Map<String, Object> graph = root();
        graph.put("@graph", List.of(app, breadcrumbs(site,
                new String[]{"Jolt", site.url("/")},
                new String[]{"Parsel", site.url("/parsel")})));
        return GSON.toJson(graph);
    }

    private static Map<String, Object> breadcrumbsRoot(JoltSite site, String[]... crumbs) {
        Map<String, Object> m = breadcrumbs(site, crumbs);
        m.put("@context", "https://schema.org");
        return m;
    }

    private static Map<String, Object> breadcrumbs(JoltSite site, String[]... crumbs) {
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("@type", "BreadcrumbList");
        List<Map<String, Object>> items = new ArrayList<>();
        int position = 1;
        for (String[] c : crumbs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", position++);
            item.put("name", c[0]);
            item.put("item", c[1]);
            items.add(item);
        }
        list.put("itemListElement", items);
        return list;
    }
}
