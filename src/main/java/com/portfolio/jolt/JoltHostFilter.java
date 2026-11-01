package com.portfolio.jolt;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Serves two websites from this one application, chosen by the request's host name.
 *
 * <p>Requests for a Jolt host (see {@link JoltSite}) are forwarded internally to the same path under
 * {@code /jolt}, so {@code jolt.williamsolari.com/docs} is answered by the controller mapped to
 * {@code /jolt/docs}, and {@code jolt.williamsolari.com/css/jolt.css} by the resource
 * {@code static/jolt/css/jolt.css}. The browser never sees the prefix.
 *
 * <p>Every other host gets the portfolio untouched, except that the {@code /jolt} prefix itself is hidden
 * there (404), so the Jolt pages cannot be reached under the portfolio's domain as duplicate content.
 *
 * <p>The order puts this filter just after the Spring Security chain (order -100): the request has already
 * received its security headers and CSRF handling before it is forwarded. The forward itself is not
 * filtered again (filters are registered for REQUEST dispatches only), so there is no loop.
 */
@Component
@Order(-99)
@RequiredArgsConstructor
public class JoltHostFilter extends OncePerRequestFilter {

    static final String PREFIX = "/jolt";

    private final JoltSite site;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // getServletPath() is the decoded path within the application (the dispatcher servlet is mapped to "/").
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }

        if (site.isJoltHost(request.getServerName())) {
            // One URL per page: "/docs/" is not a separate page, send it to "/docs" (which itself goes to the first guide).
            if (path.length() > 1 && path.endsWith("/")) {
                String clean = path.replaceAll("/+$", "");
                String query = request.getQueryString();
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location", (clean.isEmpty() ? "/" : clean) + (query == null ? "" : "?" + query));
                return;
            }
            String target = "/".equals(path) ? PREFIX : PREFIX + path;
            request.getRequestDispatcher(target).forward(request, response);
            return;
        }

        if (path.equals(PREFIX) || path.startsWith(PREFIX + "/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }
}
