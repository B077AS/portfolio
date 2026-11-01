package com.williamsolari.core.web;

/**
 * A website hosted by this application on its own host name(s), next to the default one (the portfolio).
 * {@link SiteRoutingFilter} forwards the requests of its hosts to the controllers and static resources
 * under {@link #pathPrefix()}.
 */
public interface SiteHost {

    /** Internal path prefix the site lives under, e.g. {@code /jolt}. Never visible to browsers. */
    String pathPrefix();

    /** Whether the request's host name (no port) belongs to this site. */
    boolean ownsHost(String host);
}
