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
            toggle.classList.toggle("open", isOpen);
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

    // Project coverflow: one card is centered and shows its details, the rest tilt away showing
    // their fronts. It advances on its own (paused on hover/focus, offscreen, or reduced motion),
    // and can be driven by clicking a side card, the dots, the arrow keys, or a swipe.
    var projectGrid = document.querySelector(".project-grid");
    var projectCards = projectGrid ? Array.prototype.slice.call(projectGrid.querySelectorAll(".project-card")) : [];

    if (projectGrid && projectCards.length > 1) {
        var count = projectCards.length;
        var current = 0;
        var timer = null;
        var paused = false;
        var visible = true;
        var autoplay = !window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        var narrowQuery = window.matchMedia("(max-width: 720px)");

        projectGrid.classList.add("is-coverflow");

        var dots = document.createElement("div");
        dots.className = "project-dots";
        projectCards.forEach(function (card, i) {
            var dot = document.createElement("button");
            dot.type = "button";
            dot.className = "project-dot";
            dot.setAttribute("aria-label", "Show " + card.querySelector("h3").textContent);
            dot.addEventListener("click", function () {
                goTo(i);
            });
            dots.appendChild(dot);
        });
        projectGrid.parentNode.insertBefore(dots, projectGrid.nextSibling);
        var dotButtons = Array.prototype.slice.call(dots.children);

        var paint = function () {
            var narrow = narrowQuery.matches;
            var half = Math.floor(count / 2);
            var width = projectCards[0].offsetWidth;
            var step = width * (narrow ? 0.7 : 0.83);
            var reach = narrow ? 1 : 2;

            projectCards.forEach(function (card, i) {
                var offset = ((i - current + count + half) % count) - half;
                var dist = Math.abs(offset);
                var active = offset === 0;
                var scale = active ? 1 : 0.82 - dist * 0.04;

                card.classList.toggle("is-active", active);
                card.style.transform = "translateX(-50%) translateX(" + offset * step + "px) scale(" + scale + ")";
                card.style.opacity = dist > reach ? 0 : active ? 1 : 0.75 - dist * 0.2;
                card.style.zIndex = 10 - dist;
                card.style.pointerEvents = dist > reach ? "none" : "auto";
                card.setAttribute("aria-hidden", dist > reach ? "true" : "false");

                // The details side is only reachable (links, focus) while it is the one showing.
                var back = card.querySelector(".project-card-face-back");
                if (back) back.inert = !active;
                var front = card.querySelector(".project-card-face-front");
                if (front) front.inert = active;
            });

            dotButtons.forEach(function (dot, i) {
                dot.setAttribute("aria-current", i === current ? "true" : "false");
            });
        };

        var schedule = function () {
            clearTimeout(timer);
            if (!autoplay || paused || !visible) return;
            // Long enough to read the description of the centered card.
            timer = setTimeout(function () {
                goTo(current + 1);
            }, 7000);
        };

        var goTo = function (index) {
            current = ((index % count) + count) % count;
            paint();
            schedule();
        };

        projectCards.forEach(function (card, i) {
            card.addEventListener("click", function (e) {
                if (i === current) return;
                // A side card's links are inert, but a click on its front should only bring it forward.
                e.preventDefault();
                goTo(i);
            });
        });

        projectGrid.addEventListener("mouseenter", function () {
            paused = true;
            schedule();
        });
        projectGrid.addEventListener("mouseleave", function () {
            paused = false;
            schedule();
        });
        projectGrid.addEventListener("focusin", function () {
            paused = true;
            schedule();
        });
        projectGrid.addEventListener("focusout", function () {
            paused = false;
            schedule();
        });
        projectGrid.addEventListener("keydown", function (e) {
            if (e.key === "ArrowRight") goTo(current + 1);
            else if (e.key === "ArrowLeft") goTo(current - 1);
        });

        var touchStartX = null;
        projectGrid.addEventListener("touchstart", function (e) {
            touchStartX = e.touches[0].clientX;
        }, { passive: true });
        projectGrid.addEventListener("touchend", function (e) {
            if (touchStartX === null) return;
            var dx = e.changedTouches[0].clientX - touchStartX;
            touchStartX = null;
            if (Math.abs(dx) > 40) goTo(current + (dx < 0 ? 1 : -1));
        }, { passive: true });

        if ("IntersectionObserver" in window) {
            new IntersectionObserver(function (entries) {
                visible = entries[0].isIntersecting;
                schedule();
            }).observe(projectGrid);
        }

        window.addEventListener("resize", paint);
        paint();
        schedule();
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
