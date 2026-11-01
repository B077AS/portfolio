package com.portfolio.controller;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.portfolio.jolt.JoltDocs;
import com.portfolio.jolt.JoltSite;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ErrorPageController implements ErrorController {

    private final JoltSite joltSite;
    private final JoltDocs joltDocs;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        // The Jolt site keeps real status codes (a redirect to "/" would turn every bad URL into a soft 404 for search engines).
        if (joltSite.isJoltHost(request.getServerName())) {
            Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            int status = attribute instanceof Integer code && code >= 400 ? code : 404;
            response.setStatus(status);
            model.addAttribute("site", joltSite);
            model.addAttribute("docGroups", joltDocs.groups());
            model.addAttribute("nav", "");
            model.addAttribute("noindex", true);
            model.addAttribute("status", status);
            model.addAttribute("title", status == 404 ? "Page not found | Jolt" : "Something went wrong | Jolt");
            model.addAttribute("description", "This page of the Jolt site could not be shown.");
            model.addAttribute("canonicalUrl", joltSite.url("/"));
            model.addAttribute("ogType", "website");
            model.addAttribute("ogImage", joltSite.url("/og-image.png"));
            return "jolt/error";
        }
        return "redirect:/";
    }
}
