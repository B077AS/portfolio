/* Jolt site scripts. Everything here is progressive enhancement: the pages are complete server-rendered HTML. */
(function () {
  'use strict';
  document.documentElement.classList.add('js');
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  /* ---------- theme switch (the early script in the layout already set data-theme) ---------- */
  var themeBtn = $('#theme-toggle');
  if (themeBtn) themeBtn.addEventListener('click', function () {
    var next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
    document.documentElement.dataset.theme = next;
    try { localStorage.setItem('jolt-theme', next); } catch (e) {}
    var m = $('meta[name="theme-color"]');
    if (m) m.setAttribute('content', next === 'light' ? '#faf8f5' : '#0c0d10');
  });

  /* ---------- tiny syntax highlighter (java, js, html, bash) ---------- */
  var KW = /^(abstract|assert|async|await|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|function|if|implements|import|instanceof|int|interface|let|long|new|null|of|package|private|protected|public|record|return|short|static|super|switch|this|throw|throws|try|typeof|var|void|while|yield|true|false)$/;
  function esc(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }
  function span(c, s) { return '<span class="' + c + '">' + esc(s) + '</span>'; }

  function hlCode(src) {
    var re = /(\/\/[^\n]*|\/\*[\s\S]*?\*\/)|("""[\s\S]*?"""|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|(\b\d[\d_.]*\b)|(@?[A-Za-z_$][\w$]*)(\s*\()?/g;
    var out = '', last = 0, m;
    while ((m = re.exec(src))) {
      out += esc(src.slice(last, m.index));
      if (m[1]) out += span('c', m[1]);
      else if (m[2]) {
        if (m[2].slice(0, 3) === '"""') {
          // a Java text block: the quotes are a string, the HTML inside gets markup colours
          var body = m[2].slice(3, -3);
          out += span('s', '"""') + (body.indexOf('<') >= 0 ? hlMarkup(body) : span('s', body)) + span('s', '"""');
        } else out += span('s', m[2]);
      }
      else if (m[3]) out += span('n', m[3]);
      else if (m[4]) {
        if (KW.test(m[4])) out += span('k', m[4]);
        else if (m[5]) out += span('f', m[4]);
        else out += esc(m[4]);
        if (m[5]) out += esc(m[5]);
      }
      last = re.lastIndex;
    }
    return out + esc(src.slice(last));
  }

  function hlMarkup(src) {
    var re = /(<!--[\s\S]*?-->)|(<\/?[A-Za-z][^>]*>)/g, out = '', last = 0, m;
    while ((m = re.exec(src))) {
      out += esc(src.slice(last, m.index));
      if (m[1]) out += span('c', m[1]);
      else {
        // attribute values first: wrapping the tag name afterwards adds a class="..." that must not be matched
        out += esc(m[2])
          .replace(/"([^"]*)"/g, function (_, v) { return '<span class="s">"' + v + '"</span>'; })
          .replace(/^(&lt;\/?)([A-Za-z][\w-]*)/, '$1<span class="k">$2</span>');
      }
      last = re.lastIndex;
    }
    return out + esc(src.slice(last));
  }

  function hlShell(src) {
    return src.split('\n').map(function (l) {
      var i = l.indexOf('#');
      return i >= 0 ? esc(l.slice(0, i)) + span('c', l.slice(i)) : esc(l);
    }).join('\n');
  }

  var LABELS = { java: 'Java', js: 'JavaScript', html: 'HTML', xml: 'XML', bash: 'Shell', powershell: 'PowerShell', text: '' };

  /* ---------- the page scrolls inside #scroller, so keep the window itself (and the navbar) in place ---------- */
  var resetRoot = function () { window.scrollTo(0, 0); document.documentElement.scrollTop = 0; document.body.scrollTop = 0; };
  window.addEventListener('load', resetRoot);
  window.addEventListener('hashchange', resetRoot);

  /* ---------- mobile navigation: a sidebar from the right over a dimmed page ---------- */
  var toggle = $('.nav-toggle'), links = $('nav .links'), backdrop = $('#nav-backdrop');
  var setMenuOpen = function (open) {
    if (!toggle || !links) return;
    links.classList.toggle('open', open);
    toggle.classList.toggle('open', open);
    if (backdrop) backdrop.classList.toggle('open', open);
    document.body.classList.toggle('menu-open', open);
    toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
  };
  if (toggle && links) {
    toggle.addEventListener('click', function () { setMenuOpen(!links.classList.contains('open')); });
    if (backdrop) backdrop.addEventListener('click', function () { setMenuOpen(false); });
    links.addEventListener('click', function (e) { if (e.target.closest('a')) setMenuOpen(false); });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && links.classList.contains('open')) { setMenuOpen(false); toggle.focus(); }
    });
    // growing the window past the breakpoint must not leave the page locked
    window.addEventListener('resize', function () { if (window.innerWidth > 1000) setMenuOpen(false); });
  }

  /* ---------- everything that belongs to the page content: run again after each swap ---------- */
  function initPage() {
    $$('.code[data-lang]').forEach(function (box) {
      var lang = box.getAttribute('data-lang'), codeEl = $('code', box);
      if (!codeEl) return;
      var raw = codeEl.textContent.replace(/\n$/, '');
      codeEl.innerHTML = (lang === 'html' || lang === 'xml') ? hlMarkup(raw)
        : (lang === 'bash' || lang === 'powershell') ? hlShell(raw)
        : lang === 'text' ? esc(raw) : hlCode(raw);

      var cap = document.createElement('div');
      cap.className = 'cap';
      var label = box.getAttribute('data-file') || LABELS[lang] || lang;
      cap.innerHTML = '<span></span>';
      cap.firstChild.textContent = label;
      var btn = document.createElement('button');
      btn.type = 'button'; btn.className = 'copy'; btn.textContent = 'Copy';
      btn.addEventListener('click', function () {
        if (navigator.clipboard) navigator.clipboard.writeText(raw);
        btn.textContent = 'Copied';
        setTimeout(function () { btn.textContent = 'Copy'; }, 1300);
      });
      cap.appendChild(btn);
      box.insertBefore(cap, box.firstChild);
    });


    /* ---------- hero: the three layers ---------- */
    var stack = $('#stack');
    if (stack) {
      var plates = $$('.plate', stack), infos = $$('.layer-info');
      var pick = function (i) {
        plates.forEach(function (p) { p.classList.toggle('on', +p.getAttribute('data-i') === i); });
        infos.forEach(function (n) { n.classList.toggle('on', +n.getAttribute('data-i') === i); });
      };
      plates.forEach(function (p) {
        var i = +p.getAttribute('data-i');
        p.addEventListener('mouseenter', function () { pick(i); });
        p.addEventListener('focus', function () { pick(i); });
        p.addEventListener('click', function () { pick(i); });
      });
    }

    /* ---------- screenshot carousels: one image at a time ---------- */
    $$('[data-gallery]').forEach(function (g) {
      var slides = $$('figure', g), cap = $('.gal-cap', g), count = $('.gal-count', g), i = 0;
      if (slides.length < 2) { var bar = $('.gal-bar', g); if (bar) bar.style.display = 'none'; }
      var show = function (n) {
        i = (n + slides.length) % slides.length;
        slides.forEach(function (f, k) { f.hidden = k !== i; });
        var img = $('img', slides[i]);
        if (cap && img) cap.textContent = img.alt;
        if (count) count.textContent = (i + 1) + ' / ' + slides.length;
      };
      var prev = $('[data-prev]', g), next = $('[data-next]', g);
      if (prev) prev.addEventListener('click', function () { show(i - 1); });
      if (next) next.addEventListener('click', function () { show(i + 1); });
      g.addEventListener('keydown', function (e) {
        if (e.key === 'ArrowLeft') show(i - 1); else if (e.key === 'ArrowRight') show(i + 1);
      });
      show(0);
    });

    /* ---------- parsel feature explorer ---------- */
    var ex = $('[data-explorer]');
    if (ex) {
      var items = $$('.fitem', ex), panels = $$('.fpanel', ex);
      var show = function (i) {
        items.forEach(function (b, k) { b.classList.toggle('on', k === i); b.setAttribute('aria-selected', k === i ? 'true' : 'false'); });
        panels.forEach(function (p, k) { p.hidden = k !== i; });
      };
      items.forEach(function (b, i) { b.addEventListener('click', function () { show(i); }); });
      show(0);
    }

    /* ---------- changelog: filter by type ---------- */
    var clFilter = $('.cl-filter');
    if (clFilter) {
      var clButtons = $$('button', clFilter), clSecs = $$('.cl .sec'), clRels = $$('.cl .rel');
      clButtons.forEach(function (b) {
        b.addEventListener('click', function () {
          var kind = b.getAttribute('data-kind');
          clButtons.forEach(function (x) { x.classList.toggle('on', x === b); });
          clSecs.forEach(function (s) { s.hidden = kind !== 'all' && s.getAttribute('data-kind') !== kind; });
          clRels.forEach(function (r) { r.hidden = !$$('.sec', r).some(function (s) { return !s.hidden; }); });
        });
      });
    }

    /* ---------- docs: table of contents + active section ---------- */
    var toc = $('.toc .toclinks'), article = $('.doc');
    if (toc && article) {
      var heads = $$('h2[id]', article);
      heads.forEach(function (h) {
        var a = document.createElement('a');
        a.href = '#' + h.id; a.textContent = h.textContent;
        toc.appendChild(a);
      });
      if (!heads.length) { toc.parentNode.style.display = 'none'; }
    }
  }

  /* ---------- page-to-page navigation without a full reload ----------
     A normal link click unloads the page and paints a blank frame before the next one (a white flash in some
     browsers). Instead the next page is fetched and only #scroller (page + footer) is swapped; the navbar never
     goes away. Anything unusual falls back to a normal navigation, so this is purely an enhancement. */
  var scroller = $('#scroller');
  var navToken = 0;
  var shown = location.pathname + location.search; // the page currently in #scroller

  function sameOriginLink(a, e) {
    if (!a || !a.href || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return false;
    if (a.target && a.target !== '_self') return false;
    if (a.hasAttribute('download') || a.origin !== location.origin) return false;
    // same page, only the #fragment differs: the browser handles it (scrolling to the element)
    if (a.pathname === location.pathname && a.search === location.search) return false;
    return true;
  }

  function swapPage(doc, url, push) {
    document.title = doc.title;
    ['meta[name="description"]', 'meta[name="robots"]', 'link[rel="canonical"]', 'meta[property^="og:"]', 'meta[name^="twitter:"]']
      .forEach(function (sel) {
        $$(sel, document.head).forEach(function (n) { n.remove(); });
        $$(sel, doc.head).forEach(function (n) { document.head.appendChild(document.importNode(n, true)); });
      });
    $$('script[type="application/ld+json"]', document.head).forEach(function (n) { n.remove(); });
    $$('script[type="application/ld+json"]', doc.head).forEach(function (n) { document.head.appendChild(document.importNode(n, true)); });

    // navbar: keep the element (and its listeners), only move the "current page" mark
    var fresh = $$('nav .links a', doc);
    $$('nav .links a').forEach(function (a) {
      var match = fresh.filter(function (b) { return b.getAttribute('href') === a.getAttribute('href'); })[0];
      a.className = match ? match.className : '';
    });
    setMenuOpen(false);

    scroller.innerHTML = $('#scroller', doc).innerHTML;
    if (push) history.pushState(null, '', url);
    shown = location.pathname + location.search;
    var target = location.hash && document.getElementById(decodeURIComponent(location.hash.slice(1)));
    if (target) target.scrollIntoView(); else scroller.scrollTop = 0;
    resetRoot();
    initPage();
  }

  function go(url, push) {
    var token = ++navToken;
    fetch(url, { headers: { 'X-Requested-With': 'jolt-nav' }, credentials: 'same-origin' })
      .then(function (res) {
        var type = res.headers.get('content-type') || '';
        if (!res.ok || type.indexOf('text/html') < 0) throw new Error('not a page');
        return res.text().then(function (html) { return { html: html, url: res.url }; });
      })
      .then(function (r) {
        if (token !== navToken) return; // a newer click won
        var doc = new DOMParser().parseFromString(r.html, 'text/html');
        if (!$('#scroller', doc)) throw new Error('not a Jolt page');
        var finalUrl = new URL(r.url);
        var target = finalUrl.pathname + finalUrl.search + (finalUrl.hash || new URL(url, location.href).hash);
        var apply = function () { swapPage(doc, target, push); };
        if (document.startViewTransition) document.startViewTransition(apply); else apply();
      })
      .catch(function () { location.href = url; }); // errors, 404s and odd responses get a real page load
  }

  if (scroller && window.fetch && window.DOMParser && window.history && history.pushState) {
    document.addEventListener('click', function (e) {
      var a = e.target.closest && e.target.closest('a');
      if (sameOriginLink(a, e)) { e.preventDefault(); go(a.href, true); }
    });
    window.addEventListener('popstate', function () {
      // a #fragment change fires popstate too, but there is nothing to load for it
      if (location.pathname + location.search !== shown) go(location.href, false);
    });
  }

  initPage();
})();
