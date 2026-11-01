package com.williamsolari.jolt.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.williamsolari.jolt.model.DocPage;
import com.williamsolari.jolt.service.JoltDocs;
import com.williamsolari.jolt.service.JoltSite;

import lombok.RequiredArgsConstructor;

/**
 * robots.txt and sitemap.xml of the Jolt site (the portfolio has its own plain files in {@code static/portfolio}).
 * They are generated so that the host and the page list have a single source of truth: the base URL comes from
 * {@link JoltSite} and the pages (with their last-updated dates) from {@link JoltDocs}, so a new doc page appears in
 * the sitemap without touching a file.
 */
@Controller
@RequestMapping("/jolt")
@RequiredArgsConstructor
public class JoltSeoController {

    private final JoltSite site;
    private final JoltDocs docs;

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
}
