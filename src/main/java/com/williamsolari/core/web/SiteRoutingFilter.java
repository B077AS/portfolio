package com.williamsolari.core.web;

import java.io.IOException;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Serves several websites from this one application, chosen by the request's host name.
 *
 * <p>Each site owns an internal path prefix: its controllers are mapped under it, its static files live in
 * {@code static/<prefix>} and its templates in {@code templates/<prefix>}. This filter forwards every request to the
 * same path under the prefix of the site that owns the host: {@code jolt.williamsolari.com/docs} is answered by the
 * controller mapped to {@code /jolt/docs}, and {@code jolt.williamsolari.com/css/jolt.css} by the resource
 * {@code static/jolt/css/jolt.css}. A host no {@link SiteHost} claims gets the portfolio
 * ({@link #PORTFOLIO_PREFIX}), so {@code williamsolari.com/css/base.css} is {@code static/portfolio/css/base.css}.
 * The browser never sees a prefix, and asking for one explicitly is a 404 (otherwise every page would exist
 * twice, as duplicate content).
 *
 * <p>The order puts this filter just after the Spring Security chain (order -100): the request has already
 * received its security headers and CSRF handling before it is forwarded. The forward itself is not
 * filtered again (filters are registered for REQUEST dispatches only), so there is no loop.
 */
@Component
@Order(-99)
@RequiredArgsConstructor
public class SiteRoutingFilter extends OncePerRequestFilter {

    public static final String PORTFOLIO_PREFIX = "/portfolio";

    private final List<SiteHost> sites;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // getServletPath() is the decoded path within the application (the dispatcher servlet is mapped to "/").
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }

        if (isInternalPath(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String prefix = PORTFOLIO_PREFIX;
        for (SiteHost site : sites) {
            if (site.ownsHost(request.getServerName())) {
                prefix = site.pathPrefix();
                // One URL per page: "/docs/" is not a separate page, send it to "/docs".
                if (path.length() > 1 && path.endsWith("/")) {
                    String clean = path.replaceAll("/+$", "");
                    String query = request.getQueryString();
                    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                    response.setHeader("Location", (clean.isEmpty() ? "/" : clean) + (query == null ? "" : "?" + query));
                    return;
                }
                break;
            }
        }

        request.getRequestDispatcher("/".equals(path) ? prefix : prefix + path).forward(request, response);
    }

    private boolean isInternalPath(String path) {
        return isUnder(path, PORTFOLIO_PREFIX) || sites.stream().anyMatch(site -> isUnder(path, site.pathPrefix()));
    }

    private static boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
