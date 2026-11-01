package com.portfolio.jolt;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** The documentation pages, in reading order. */
@Component
public class JoltDocs {

    private static final LocalDate UPDATED = LocalDate.of(2026, 10, 31);

    private final List<DocPage> pages = List.of(
            page("getting-started", "Getting started", "Start",
                    "Add Jolt to a Maven or Gradle project and open your first native window with a web page, in plain Java."),
            page("messaging", "Messaging between Java and the page", "Start",
                    "Send messages both ways between your Java code and the page, and build a small request and response layer on top."),
            page("frontend", "Building the frontend", "Start",
                    "Use plain HTML or any frontend framework with Jolt: dev server with hot reload while you work, files from the jar for release."),
            page("router", "Serving files and requests", "Serving",
                    "Serve a multi-file frontend and answer fetch, form and htmx requests from Java with the Router, with no socket."),
            page("channels", "Binary channels", "Serving",
                    "Open named two-way channels to move video frames, images and files between Java and the page without base64 or JSON."),
            page("dialogs", "File dialogs", "Desktop",
                    "Open the native open, save and folder dialogs from Java or from the page, without the page ever seeing a file path."),
            page("notifications", "System notifications", "Desktop",
                    "Show native toast notifications with click actions, buttons and reply fields on Windows and Linux."),
            page("popout-windows", "Pop-out windows", "Desktop",
                    "Let a page ask for another window while your Java code decides, with keys to avoid duplicates and broadcasts between windows."),
            page("window-options", "Window look and placement", "Desktop",
                    "Set size, icon, theme and start position of a window, and draw your own title bar with frameless windows."),
            page("tray", "Tray icon, close to tray and autostart", "Desktop",
                    "Add a tray icon and menu, hide the window on close, start with the computer and allow only one copy per user."),
            page("security", "Security", "Reference",
                    "How the SecurityPolicy keeps untrusted pages away from your Java code: navigation lock, message check and more."),
            page("credentials", "Saving passwords and tokens", "Reference",
                    "Store secrets in the Windows Credential Manager or the Linux Secret Service with the optional jolt-credentials module."),
            page("packaging", "Packaging your app", "Reference",
                    "Turn your jars into a double-clickable app that bundles its own Java runtime with jpackage, about 32 MB in size."),
            page("troubleshooting", "Troubleshooting", "Reference",
                    "Common symptoms when running Jolt apps, such as a blank window or a missing web view, and what fixes them."));

    public List<DocPage> pages() {
        return pages;
    }

    public Optional<DocPage> find(String slug) {
        return pages.stream().filter(p -> p.slug().equals(slug)).findFirst();
    }

    /** Pages grouped for the sidebar, keeping the reading order. */
    public Map<String, List<DocPage>> groups() {
        Map<String, List<DocPage>> groups = new LinkedHashMap<>();
        for (DocPage p : pages) {
            groups.computeIfAbsent(p.group(), g -> new java.util.ArrayList<>()).add(p);
        }
        return groups;
    }

    public Optional<DocPage> previous(DocPage page) {
        int i = pages.indexOf(page);
        return i > 0 ? Optional.of(pages.get(i - 1)) : Optional.empty();
    }

    public Optional<DocPage> next(DocPage page) {
        int i = pages.indexOf(page);
        return i >= 0 && i < pages.size() - 1 ? Optional.of(pages.get(i + 1)) : Optional.empty();
    }

    private static DocPage page(String slug, String title, String group, String description) {
        return new DocPage(slug, title, group, description, UPDATED);
    }
}
