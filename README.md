<h1 align="center">
  <img src="src/main/resources/static/favicon.svg" alt="Portfolio icon" width="64"><br>
  Portfolio
</h1>

<p align="center">
  <b>William Solari's personal portfolio</b> - a Spring Boot + Thymeleaf site showcasing my projects.
</p>

<p align="center">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white">
  <img alt="Thymeleaf" src="https://img.shields.io/badge/Thymeleaf-005F0F?logo=thymeleaf&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white">
  <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

---

## About

This is the source for [williamsolari.com](https://williamsolari.com) - a personal portfolio site built with Spring Boot and Thymeleaf, listing my projects and how to get in touch.

## Tech stack

- **Java 25** / **Spring Boot 4.1** - Spring MVC, Spring Security, Spring Mail, Bean Validation
- **Thymeleaf** - server-rendered templates, with the layout dialect for shared page structure
- **Vanilla CSS/JS** - no frontend framework; [Material Design Icons](https://materialdesignicons.com/) and [IBM Plex](https://fontsource.org/fonts/ibm-plex-sans) fonts, bundled at build time via a Maven-driven npm step

## Running locally

Requirements: **Java 25** and Maven (or the bundled `mvnw`).

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

## License

This project is licensed under the [MIT License](LICENSE).

## Jolt showcase site (second website, same app)

`jolt.williamsolari.com` is served by this same Spring Boot application, with its own design, SEO, robots.txt and sitemap.

- `com.portfolio.jolt.JoltHostFilter` looks at the request host. For a Jolt host it forwards every request internally to the same path under `/jolt`
  (so `jolt.williamsolari.com/docs` is `JoltController` at `/jolt/docs`, and `/css/jolt.css` is `static/jolt/css/jolt.css`). Other hosts get the portfolio; `/jolt` itself is hidden there (404).
- Pages: `templates/jolt/` (layout, home, parsel, docs). Documentation bodies are one fragment per page in `templates/jolt/docs/<slug>.html`; the list and descriptions are in `JoltDocs`.
- Config (`application.properties`): `site.jolt.hosts`, `site.jolt.base-url`, `site.jolt.version-url` (the Jolt version shown on the site is the latest GitHub release, refreshed hourly; there is no version property). `server.forward-headers-strategy=framework` makes the app trust `X-Forwarded-*` from nginx.
- Local preview: run the app and open `http://jolt.localhost:8080` (browsers resolve `*.localhost` to your machine, no hosts-file change).
- nginx: proxy both server names to the app and keep the original host: `proxy_set_header Host $host;` plus `X-Forwarded-Proto` and `X-Forwarded-Host`. The certificate must cover `jolt.williamsolari.com`.
