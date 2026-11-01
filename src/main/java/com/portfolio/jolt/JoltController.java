package com.portfolio.jolt;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Pages of the Jolt showcase site. Mapped under {@code /jolt}; {@link JoltHostFilter} makes them answer at the
 * root of the Jolt host (so {@code /jolt/docs} is {@code jolt.williamsolari.com/docs}).
 */
@Controller
@RequestMapping("/jolt")
@RequiredArgsConstructor
public class JoltController {

    private static final String HOME_TITLE = "Jolt - the Java framework for desktop apps with web UIs";
    private static final String HOME_DESCRIPTION =
            "Jolt is an open source Java framework for desktop apps with web UIs. A native window, the web view your OS "
                    + "already has, and Java talking to your page in one process. No Electron, no JNI. Windows and Linux.";
    private static final String CHANGELOG_TITLE = "Changelog | Jolt";
    private static final String CHANGELOG_DESCRIPTION =
            "Every released version of Jolt, newest first: what was added, changed and fixed in each release of the Java "
                    + "framework for desktop apps with web UIs.";
    private static final String PARSEL_TITLE = "Parsel - a desktop mail client built with Jolt";
    private static final String PARSEL_DESCRIPTION =
            "Parsel is a clean desktop mail client for your own mailbox, built on the Jolt framework: Java for IMAP and "
                    + "SMTP, a Vue page for the interface, one process, a 33 MB installer. Windows and Linux.";

    /** A Parsel screenshot: file name under /img/parsel and its description. */
    public record Shot(String file, String alt) {
    }

    private static final List<Shot> HOME_SHOTS = List.of(
            new Shot("inbox-dark-violet.png", "Parsel in dark mode with the violet accent, showing the inbox and a message"),
            new Shot("compose-dark-teal.png", "Parsel's rich text composer in dark mode with the teal accent"),
            new Shot("inbox-light-blue.png", "Parsel in light mode with the blue accent"),
            new Shot("settings-light-orange.png", "Parsel settings, appearance, with the eight accent colors"));

    private static final List<Shot> PARSEL_SHOTS = List.of(
            new Shot("inbox-dark-violet.png", "Parsel in dark mode with the violet accent, showing the inbox and a message"),
            new Shot("inbox-light-blue.png", "Parsel in light mode with the blue accent"),
            new Shot("compose-dark-teal.png", "The rich text composer in dark mode with the teal accent"),
            new Shot("invoice-light-red.png", "A message with an attachment, with the red accent"),
            new Shot("settings-light-orange.png", "Settings, appearance, with the eight accent colors"),
            new Shot("first-run-light-green.png", "Connecting the first mailbox, with the green accent"));

    private final JoltSite site;
    private final JoltDocs docs;
    private final JoltChangelog changelog;

    @ModelAttribute
    void shared(Model model) {
        model.addAttribute("site", site);
        model.addAttribute("docGroups", docs.groups());
    }

    @GetMapping
    String home(Model model) {
        seo(model, "home", HOME_TITLE, HOME_DESCRIPTION, "/", "website", JoltJsonLd.home(site, HOME_DESCRIPTION));
        model.addAttribute("homeShots", HOME_SHOTS);
        model.addAttribute("firstDocs", List.of(
                docs.find("getting-started").orElseThrow(),
                docs.find("messaging").orElseThrow(),
                docs.find("popout-windows").orElseThrow()));
        return "jolt/home";
    }

    /** There is no docs index page: /docs goes straight to the first guide. */
    @GetMapping("/docs")
    ResponseEntity<Void> docs() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, "/docs/getting-started").build();
    }

    @GetMapping("/docs/{slug}")
    String doc(@PathVariable String slug, Model model) {
        DocPage page = docs.find(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        seo(model, "docs", page.title() + " | Jolt documentation", page.description(), page.path(), "article",
                JoltJsonLd.doc(site, page));
        model.addAttribute("doc", page);
        model.addAttribute("prev", docs.previous(page).orElse(null));
        model.addAttribute("next", docs.next(page).orElse(null));
        return "jolt/doc";
    }

    @GetMapping("/parsel")
    String parsel(Model model) {
        seo(model, "parsel", PARSEL_TITLE, PARSEL_DESCRIPTION, "/parsel", "website",
                JoltJsonLd.parsel(site, PARSEL_DESCRIPTION));
        model.addAttribute("parselShots", PARSEL_SHOTS);
        return "jolt/parsel";
    }

    @GetMapping("/changelog")
    String changelog(Model model) {
        seo(model, "changelog", CHANGELOG_TITLE, CHANGELOG_DESCRIPTION, "/changelog", "website", null);
        model.addAttribute("releases", changelog.releases());
        return "jolt/changelog";
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String robots() {
        return "User-agent: *\nAllow: /\n\nSitemap: " + site.url("/sitemap.xml") + "\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    String sitemap() {
        String lastmod = docs.pages().get(0).updated().toString();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        entry(xml, site.url("/"), lastmod);
        for (DocPage p : docs.pages()) {
            entry(xml, site.url(p.path()), p.updated().toString());
        }
        entry(xml, site.url("/parsel"), lastmod);
        entry(xml, site.url("/changelog"), lastmod);
        return xml.append("</urlset>\n").toString();
    }

    private static void entry(StringBuilder xml, String loc, String lastmod) {
        xml.append("  <url><loc>").append(loc).append("</loc><lastmod>").append(lastmod).append("</lastmod></url>\n");
    }

    private void seo(Model model, String nav, String title, String description, String path, String ogType, String jsonLd) {
        model.addAttribute("nav", nav);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("canonicalUrl", site.url(path));
        model.addAttribute("ogType", ogType);
        model.addAttribute("ogImage", site.url("/og-image.png"));
        model.addAttribute("jsonLd", jsonLd);
    }
}
