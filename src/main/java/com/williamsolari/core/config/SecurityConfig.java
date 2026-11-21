package com.williamsolari.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.williamsolari.core.web.SiteHost;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, List<SiteHost> sites) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()))
                // Spring Security defers CsrfToken resolution - without this, the token
                // rendered into the form is never actually written as the XSRF-TOKEN cookie.
                .addFilterAfter(new CsrfCookieFilter(sites), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * Writes the XSRF-TOKEN cookie, but only for the default site (the portfolio, which has the contact form). The
     * other sites (see {@link SiteHost}) have no forms, so they get no cookie at all and their privacy notices can say so.
     */
    private static class CsrfCookieFilter extends OncePerRequestFilter {
        private final List<SiteHost> sites;

        CsrfCookieFilter(List<SiteHost> sites) {
            this.sites = sites;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {
            boolean otherSite = sites.stream().anyMatch(site -> site.ownsHost(request.getServerName()));
            CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
            if (csrfToken != null && !otherSite) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
