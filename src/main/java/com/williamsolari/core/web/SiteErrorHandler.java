package com.williamsolari.core.web;

import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Renders the error page of one hosted site. {@link ErrorPageController} asks each handler in turn and
 * falls back to its default behaviour (the portfolio redirect to the home page) when none claims the host.
 */
public interface SiteErrorHandler {

    boolean handles(HttpServletRequest request);

    /** Fills the model, sets the response status and returns the view name. */
    String render(HttpServletRequest request, HttpServletResponse response, Model model);
}
