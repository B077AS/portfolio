package com.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.portfolio.service.ProjectCatalog;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProjectCatalog projectCatalog;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("projects", projectCatalog.getProjects());
        return "index";
    }
}
