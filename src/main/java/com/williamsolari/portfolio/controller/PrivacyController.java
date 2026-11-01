package com.williamsolari.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Mapped under /portfolio; SiteRoutingFilter makes it answer at /privacy on the portfolio host. */
@Controller
@RequestMapping("/portfolio")
public class PrivacyController {

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("subpage", true);
        model.addAttribute("title", "Privacy - William Solari");
        model.addAttribute("description", "What this site collects when you use the contact form, and what it does not.");
        model.addAttribute("canonicalUrl", "https://williamsolari.com/privacy");
        return "portfolio/privacy";
    }
}
