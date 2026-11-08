package com.williamsolari.jolt.service;

import com.williamsolari.jolt.model.DocPage;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** The documentation pages, in reading order. */
@Component
public class JoltDocs {

    private static final LocalDate UPDATED = LocalDate.of(2026, 11, 8);

    private final List<DocPage> pages = List.of(
            page("getting-started", "Getting started", "Start",
                    "Add Jolt to a Maven or Gradle project and open your first native window with a web page, in plain Java."),
            page("concepts", "How Jolt works", "Start",
                    "The few ideas behind every Jolt app in plain terms: application, window, the UI thread, and the ways to show a page and talk to it."),
            page("messaging", "Messaging between Java and the page", "Start",
                    "Send messages both ways between Java and the page, step by step from one string to calling Java functions like async functions."),
            page("frontend", "Building the frontend", "Start",
                    "Put your page in HTML, CSS and JS files and show it: from a folder or a dev server with hot reload while you work, from the jar for release."),
            page("tutorial", "Tutorial: a notes app", "Start",
                    "Build a small notes app step by step: a Java data class, a page of separate files and the bridge between them."),
            page("router", "Serving files and requests", "Serving",
                    "Let the page talk to Java like a website talks to a server: fetch, forms and htmx answered by a handler or the Router, with no socket."),
            page("channels", "Binary channels", "Serving",
                    "Open named two-way channels to move video frames, images and files between Java and the page without base64 or JSON."),
            page("dialogs", "File dialogs", "Desktop",
                    "Open the native open, save and folder dialogs from Java or from the page, without the page ever seeing a file path."),
            page("notifications", "System notifications", "Desktop",
                    "Show native toast notifications with click actions, buttons and reply fields on Windows and Linux."),
            page("clipboard", "Clipboard", "Desktop",
                    "Copy and paste from Java: text first, then HTML, images and files, secrets that vanish after 30 seconds, and change events."),
            page("file-drag-drop", "Dragging files in and out", "Desktop",
                    "Receive files dropped onto a window as real paths, with several drop areas, and let users drag files out to the desktop."),
            page("popout-windows", "Pop-out windows", "Desktop",
                    "Let a page ask for another window while your Java code decides, with keys to avoid duplicates and broadcasts between windows."),
            page("window-options", "Window look and placement", "Desktop",
                    "Set size, icon, theme and start position of a window, then draw your own title bar, go full screen and get Windows 11 snap layouts."),
            page("tray", "Tray icon, close to tray and autostart", "Desktop",
                    "Add a tray icon and menu, hide the window on close, start with the computer and allow only one copy per user."),
            page("taskbar", "Taskbar progress, buttons and jump list", "Desktop",
                    "Show progress on the taskbar icon, add buttons under its Windows preview and offer actions on the icon's right-click menu."),
            page("url-schemes", "Custom links (myapp://)", "Desktop",
                    "Let links like myapp://invite/ABC open your app, read them at start-up and in a running copy, and treat them as untrusted input."),
            page("security", "Security", "Reference",
                    "How the SecurityPolicy keeps untrusted pages away from your Java code: navigation lock, message check, granting the microphone and more."),
            page("credentials", "Saving passwords and tokens", "Reference",
                    "Store secrets in the Windows Credential Manager or the Linux Secret Service with the optional jolt-credentials module."),
            page("packaging", "Packaging your app", "Reference",
                    "Turn your jars into a double-clickable app that bundles its own Java runtime with jpackage, about 32 MB in size."),
            page("api-overview", "API at a glance", "Reference",
                    "A quick lookup of the main Jolt types and calls for Java and for the page, with links to the page that explains each."),
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
