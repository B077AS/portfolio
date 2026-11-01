package com.williamsolari.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.williamsolari.portfolio.service.ProjectCatalog;

/** Mapped under /portfolio; SiteRoutingFilter makes it answer at the root of the portfolio host. */
@Controller
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class HomeController {

    private final ProjectCatalog projectCatalog;

    @GetMapping
    public String home(Model model) {
        model.addAttribute("projects", projectCatalog.getProjects());
        return "portfolio/index";
    }
}
