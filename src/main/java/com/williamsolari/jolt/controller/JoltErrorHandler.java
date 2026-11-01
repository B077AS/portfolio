package com.williamsolari.jolt.controller;

import com.williamsolari.jolt.service.JoltDocs;
import com.williamsolari.jolt.service.JoltSite;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.williamsolari.core.web.SiteErrorHandler;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Error page of the Jolt site. It keeps real status codes (a redirect to "/" would be a soft 404 for search engines). */
@Component
@RequiredArgsConstructor
public class JoltErrorHandler implements SiteErrorHandler {

    private final JoltSite site;
    private final JoltDocs docs;

    @Override
    public boolean handles(HttpServletRequest request) {
        return site.ownsHost(request.getServerName());
    }

    @Override
    public String render(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = attribute instanceof Integer code && code >= 400 ? code : 404;
        response.setStatus(status);
        model.addAttribute("site", site);
        model.addAttribute("docGroups", docs.groups());
        model.addAttribute("nav", "");
        model.addAttribute("noindex", true);
        model.addAttribute("status", status);
        model.addAttribute("title", status == 404 ? "Page not found | Jolt" : "Something went wrong | Jolt");
        model.addAttribute("description", "This page of the Jolt site could not be shown.");
        model.addAttribute("canonicalUrl", site.url("/"));
        model.addAttribute("ogType", "website");
        model.addAttribute("ogImage", site.url("/og-image.png"));
        return "jolt/pages/error";
    }
}
