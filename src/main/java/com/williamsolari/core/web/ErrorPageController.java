package com.williamsolari.core.web;

import java.util.List;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ErrorPageController implements ErrorController {

    private final List<SiteErrorHandler> siteHandlers;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response, Model model) {
        for (SiteErrorHandler handler : siteHandlers) {
            if (handler.handles(request)) {
                return handler.render(request, response, model);
            }
        }
        // The portfolio is a single page: anything unknown goes back to it.
        return "redirect:/";
    }
}
