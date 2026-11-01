/* Jolt site scripts. Everything here is progressive enhancement: the pages are complete server-rendered HTML. */
(function () {
  'use strict';
  document.documentElement.classList.add('js');
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  /* ---------- tiny syntax highlighter (java, js, html, bash) ---------- */
  var KW = /^(abstract|assert|async|await|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|function|if|implements|import|instanceof|int|interface|let|long|new|null|of|package|private|protected|public|record|return|short|static|super|switch|this|throw|throws|try|typeof|var|void|while|yield|true|false)$/;
  function esc(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }
  function span(c, s) { return '<span class="' + c + '">' + esc(s) + '</span>'; }

  function hlCode(src) {
    var re = /(\/\/[^\n]*|\/\*[\s\S]*?\*\/)|("(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'|`(?:\\.|[^`\\])*`)|(\b\d[\d_.]*\b)|(@?[A-Za-z_$][\w$]*)(\s*\()?/g;
    var out = '', last = 0, m;
    while ((m = re.exec(src))) {
      out += esc(src.slice(last, m.index));
      if (m[1]) out += span('c', m[1]);
      else if (m[2]) out += span('s', m[2]);
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

  /* ---------- the page scrolls inside #scroller, so keep the window itself (and the navbar) in place ---------- */
  var resetRoot = function () { window.scrollTo(0, 0); document.documentElement.scrollTop = 0; document.body.scrollTop = 0; };
  window.addEventListener('load', resetRoot);
  window.addEventListener('hashchange', resetRoot);

  /* ---------- mobile navigation ---------- */
  var toggle = $('.nav-toggle'), links = $('nav .links');
  if (toggle && links) {
    toggle.addEventListener('click', function () {
      var open = links.classList.toggle('open');
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }

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
})();
