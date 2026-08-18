package com.portfolio.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.portfolio.model.Project;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCatalog {

    private final Gson gson;

    @Getter
    private List<Project> projects;

    @PostConstruct
    void load() throws IOException {
        try (var reader = new InputStreamReader(
                new ClassPathResource("projects.json").getInputStream(), StandardCharsets.UTF_8)) {
            projects = gson.fromJson(reader, new TypeToken<List<Project>>() {
            }.getType());
        }
        log.info("Loaded {} projects from projects.json", projects.size());
    }

}
