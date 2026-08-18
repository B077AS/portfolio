(function () {
    "use strict";

    var main = document.querySelector("main");

    // <main> is the real scroll container (see base.css), not the document. Element.scrollIntoView()
    // walks EVERY scrolling ancestor up to the document - including <body>, which technically still
    // counts as a scroll container even with overflow:hidden - so it ends up nudging body by the
    // navbar's own height trying to align things there too, dragging the (non-scrolling) navbar out
    // of view. Scrolling `main` directly, computed from actual on-screen positions, avoids that entirely.
    function scrollToTarget(target) {
        if (!main) {
            target.scrollIntoView({ behavior: "smooth", block: "start" });
            return;
        }
        var delta = target.getBoundingClientRect().top - main.getBoundingClientRect().top;
        main.scrollTo({ top: main.scrollTop + delta, behavior: "smooth" });
    }

    document.querySelectorAll("a[href^='#']").forEach(function (link) {
        link.addEventListener("click", function (e) {
            var href = link.getAttribute("href");
            if (!href || href.length < 2) return;

            var target = document.querySelector(href);
            if (!target) return;

            e.preventDefault();
            scrollToTarget(target);

            if (window.history && window.history.pushState) {
                window.history.pushState(null, "", href);
            }
        });
    });

    // A page load that already has a hash (direct link, refresh, back/forward) triggers the
    // browser's own native fragment scroll, with the exact same bug - and it fires during the
    // "load" step, i.e. AFTER this deferred script's first pass, so the fix has to run on
    // window.load (one frame later, so it runs after the native scroll, not racing it).
    function fixHashScroll() {
        if (!main || !window.location.hash) return;
        var target = document.querySelector(window.location.hash);
        if (!target) return;
        document.scrollingElement.scrollTop = 0;
        main.scrollTop = target.getBoundingClientRect().top - main.getBoundingClientRect().top + main.scrollTop;
    }

    if (main && window.location.hash) {
        window.addEventListener("load", function () {
            requestAnimationFrame(fixHashScroll);
        });
        window.addEventListener("hashchange", fixHashScroll);
    }

    var scrollCue = document.querySelector(".scroll-cue");

    if (main && scrollCue) {
        var updateScrollCue = function () {
            scrollCue.classList.toggle("is-hidden", main.scrollTop > 10);
        };
        main.addEventListener("scroll", updateScrollCue, { passive: true });
        updateScrollCue();
    }

    var toggle = document.getElementById("navbar-toggle");
    var links = document.getElementById("navbar-links");
    var backdrop = document.getElementById("navbar-backdrop");

    if (toggle && links) {
        var setMenuOpen = function (isOpen) {
            links.classList.toggle("open", isOpen);
            toggle.setAttribute("aria-expanded", isOpen ? "true" : "false");
            if (backdrop) {
                backdrop.classList.toggle("open", isOpen);
            }
            // The sidebar overlays <main> rather than displacing it, so <main> itself (the real
            // scroll container - see the scrollToTarget comment above) needs its scroll locked,
            // not body/documentElement.
            if (main) {
                main.style.overflow = isOpen ? "hidden" : "";
            }
        };

        toggle.addEventListener("click", function () {
            setMenuOpen(!links.classList.contains("open"));
        });

        if (backdrop) {
            backdrop.addEventListener("click", function () {
                setMenuOpen(false);
            });
        }

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && links.classList.contains("open")) {
                setMenuOpen(false);
            }
        });

        links.querySelectorAll("a").forEach(function (link) {
            link.addEventListener("click", function () {
                setMenuOpen(false);
            });
        });
    }

    // Contact form is submitted via fetch instead of a classic POST-redirect-GET so the page
    // never reloads/rescrolls - a normal form submit was jumping to the top of the page and then
    // scrolling back down to #contact once the reload finished.
    var contactForm = document.getElementById("contact-form");
    var contactBanner = document.getElementById("contact-banner");
    var contactStatusTimeout = null;

    function renderContactBanner(type, message) {
        if (!contactBanner) return;
        if (contactStatusTimeout) {
            clearTimeout(contactStatusTimeout);
        }

        var icon = type === "success" ? "mdi-check-circle-outline" : "mdi-alert-circle-outline";
        contactBanner.className = "form-status form-status-" + type;
        contactBanner.innerHTML = '<i class="mdi ' + icon + '"></i><span></span>';
        contactBanner.querySelector("span").textContent = message;

        // Added in a separate frame from the content change so the transition (triggered by the
        // is-visible class) actually animates in instead of starting in its end state.
        requestAnimationFrame(function () {
            contactBanner.classList.add("is-visible");
        });

        contactStatusTimeout = setTimeout(function () {
            contactBanner.classList.remove("is-visible");
        }, 10000);
    }

    if (contactForm && contactBanner) {
        contactForm.addEventListener("submit", function (e) {
            e.preventDefault();

            var submitButton = contactForm.querySelector("button[type='submit']");
            var originalButtonHtml = submitButton ? submitButton.innerHTML : null;
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.innerHTML = '<span class="btn-spinner"></span> Sending…';
            }

            var csrfToken = document.querySelector('meta[name="_csrf"]');
            var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
            var headers = {};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader.content] = csrfToken.content;
            }

            fetch(contactForm.action, {
                method: "POST",
                credentials: "same-origin",
                headers: headers,
                body: new URLSearchParams(new FormData(contactForm))
            })
                .then(function (response) {
                    return response.json().then(function (data) {
                        return { ok: response.ok, data: data };
                    });
                })
                .then(function (result) {
                    if (result.ok) {
                        renderContactBanner("success", result.data.message);
                        contactForm.reset();
                    } else {
                        renderContactBanner("error", result.data.message);
                    }
                })
                .catch(function () {
                    renderContactBanner("error", "Something went wrong sending your message - please try again later.");
                })
                .finally(function () {
                    if (submitButton) {
                        submitButton.disabled = false;
                        submitButton.innerHTML = originalButtonHtml;
                    }
                });
        });
    }

    // Devices without a real :hover (touch) get a tap-to-flip toggle instead; taps on the
    // links revealed on the back still navigate rather than flipping the card back.
    if (window.matchMedia("(hover: none)").matches) {
        document.querySelectorAll(".project-card").forEach(function (card) {
            card.addEventListener("click", function (e) {
                if (e.target.closest("a")) return;
                card.classList.toggle("is-flipped");
            });
        });
    }

    var sections = document.querySelectorAll("main section[id]");
    var navLinks = links ? links.querySelectorAll("a[href^='#']") : [];

    if (sections.length && navLinks.length && "IntersectionObserver" in window) {
        var observer = new IntersectionObserver(
            function (entries) {
                entries.forEach(function (entry) {
                    if (!entry.isIntersecting) return;
                    var id = entry.target.getAttribute("id");
                    navLinks.forEach(function (link) {
                        link.classList.toggle("active", link.getAttribute("href") === "#" + id);
                    });
                });
            },
            { rootMargin: "-45% 0px -50% 0px" }
        );

        sections.forEach(function (section) {
            observer.observe(section);
        });
    }
})();
