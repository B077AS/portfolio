package com.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    private String slug;
    private String name;
    private String tagline;
    private String description;
    private List<String> tech;
    private String icon;
    private Link primaryLink;
    private List<Link> repos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String label;
        private String url;
    }
}
