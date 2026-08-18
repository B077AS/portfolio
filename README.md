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
