package com.williamsolari.jolt.model;

import java.time.LocalDate;

/** One page of the Jolt documentation. The body lives in {@code templates/jolt/docs/<slug>.html}. */
public record DocPage(String slug, String title, String group, String description, LocalDate updated) {

    public String path() {
        return "/docs/" + slug;
    }
}
