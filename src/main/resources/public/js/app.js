/* ═══════════════════════════════════════════════
   app.js — dashboard controller (thin)
   Results are rendered server-side (Java ResultView);
   this script wires events and injects the HTML.
   ═══════════════════════════════════════════════ */

let fullData = null, lastUrl = '';

/* ── theme ── */
const html = document.documentElement;
html.setAttribute('data-theme', localStorage.getItem('theme') || 'dark');
$('themeBtn').onclick = () => {
  const n = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', n);
  localStorage.setItem('theme', n);
};

/* ── auth guard + user menu ── */
async function initAuth() {
  try {
    const me = await Api.me();
    const name = me.name || 'User';
    $('userName').textContent = name;
    $('avatar').textContent = (name[0] || 'U').toUpperCase();
    $('ddName').textContent = name;
    $('ddEmail').textContent = me.email || '';
  } catch {
    window.location.href = '/signin';
  }
}
$('userBtn').onclick = (e) => { e.stopPropagation(); $('dropdown').classList.toggle('open'); };
document.addEventListener('click', () => $('dropdown').classList.remove('open'));
$('logoutBtn').onclick = async () => {
  try { await Api.logout(); } catch {}
  window.location.href = '/signin';
};

/* ── chips + enter key ── */
document.querySelectorAll('.chip[data-url]').forEach(c => c.onclick = () => { $('urlIn').value = c.dataset.url; });
$('urlIn').onkeydown = e => { if (e.key === 'Enter') doScrape(); };
$('scrapeBtn').onclick = doScrape;

/* ═══ PROGRESS STAGES ═══ */
const STAGES = [
  { p: 12, t: 'Connecting to site…' },
  { p: 32, t: 'Fetching HTML…' },
  { p: 55, t: 'Parsing content…' },
  { p: 74, t: 'Extracting links, images & tables…' },
  { p: 88, t: 'Building report…' },
];
let stageTimer = null;
function startProgress() {
  Util.show('progress');
  let i = 0;
  $('progressFill').style.width = '6%';
  $('progressStage').textContent = STAGES[0].t;
  stageTimer = setInterval(() => {
    if (i >= STAGES.length) return;
    $('progressFill').style.width = STAGES[i].p + '%';
    $('progressStage').textContent = STAGES[i].t;
    i++;
  }, 550);
}
function finishProgress(label) {
  clearInterval(stageTimer);
  $('progressFill').style.width = '100%';
  $('progressStage').textContent = label || 'Done';
  setTimeout(() => Util.hide('progress'), 700);
}
function abortProgress() { clearInterval(stageTimer); Util.hide('progress'); }

/* ═══ SCRAPE ═══ */
async function doScrape() {
  const url = $('urlIn').value.trim();
  if (!Util.validUrl(url)) { $('urlErr').textContent = 'Please enter a valid URL (https://...)'; return; }
  $('urlErr').textContent = '';
  Util.setLoad($('scrapeBtn'), true);
  Util.hide('statsWrap'); Util.hide('resBox'); Util.hide('fullErr');
  startProgress();

  try {
    const resp = await Api.scrapeFull(url);   // { data, statsHtml, resultHtml }
    fullData = resp.data; lastUrl = url;

    // Inject server-rendered HTML
    $('statsBar').innerHTML = resp.statsHtml;
    $('resBox').innerHTML = resp.resultHtml;

    wireResult();
    Util.show('statsWrap'); Util.show('resBox');
    finishProgress('Done · ' + (fullData.linkCount || 0) + ' links found');
    toast(`Scraped ${fullData.linkCount} links · ${fullData.imageCount} images`, 'ok');
  } catch (e) {
    abortProgress();
    if (e.status === 401) { window.location.href = '/signin'; return; }
    $('fullErr').textContent = e.message; Util.show('fullErr');
  } finally {
    Util.setLoad($('scrapeBtn'), false);
  }
}

/* Wire interactions on the freshly injected result HTML */
function wireResult() {
  const box = $('resBox');

  // tab switching (event delegation)
  box.querySelectorAll('.rtab').forEach(btn => {
    btn.onclick = () => {
      box.querySelectorAll('.rtab').forEach(t => t.classList.remove('active'));
      btn.classList.add('active');
      const t = btn.dataset.rt;
      ['meta', 'links', 'imgs', 'heads', 'tables', 'contacts', 'stats', 'shot']
        .forEach(id => { const p = $('rp-' + id); if (p) p.classList.toggle('hidden', id !== t); });
      if (t === 'shot') Util.loadShot($('shotWrap'));
    };
  });

  // copy buttons
  box.querySelectorAll('[data-copy]').forEach(b => b.onclick = () => Util.copy(b.dataset.copy));

  // per-section export dropdowns (hover via CSS; click toggles for touch)
  box.querySelectorAll('.pexport-btn').forEach(btn => {
    btn.onclick = (e) => {
      e.stopPropagation();
      const menu = btn.nextElementSibling;
      box.querySelectorAll('.pexport-menu.open').forEach(m => { if (m !== menu) m.classList.remove('open'); });
      menu.classList.toggle('open');
    };
  });
  box.querySelectorAll('[data-pex]').forEach(b => b.onclick = () => {
    box.querySelectorAll('.pexport-menu.open').forEach(m => m.classList.remove('open'));
    const [sec, fmt] = b.dataset.pex.split('|');
    sectionExport(sec, fmt);
  });
  box.querySelectorAll('.tbl-csv').forEach(b => b.onclick = () => tableCsv(parseInt(b.dataset.tbl, 10)));

  // filters
  bindFilter('lSrch', 'lList', '.li', 'lCnt');
  bindFilter('iSrch', 'iGrid', '.icard', 'iCnt');
}

function bindFilter(inputId, listId, itemSel, countId) {
  const input = $(inputId), list = $(listId);
  if (!input || !list) return;
  const items = Array.from(list.querySelectorAll(itemSel));
  const setCount = (shown) => { const c = $(countId); if (c) c.textContent = `${shown} of ${items.length}`; };
  setCount(items.length);
  input.oninput = () => {
    const q = input.value.toLowerCase();
    let shown = 0;
    items.forEach(it => {
      const match = !q || (it.dataset.search || '').includes(q);
      it.classList.toggle('hidden', !match);
      if (match) shown++;
    });
    setCount(shown);
  };
}

/* ═══ EXPORTS ═══ */
const exportDropdown = $('exportDropdown');
$('exportTrigger').onclick = (e) => { e.stopPropagation(); exportDropdown.classList.toggle('open'); };
document.addEventListener('click', () => {
  exportDropdown.classList.remove('open');
  document.querySelectorAll('.pexport-menu.open').forEach(m => m.classList.remove('open'));
});
document.querySelectorAll('.export-opt').forEach(opt => {
  opt.onclick = () => { exportDropdown.classList.remove('open'); doExport(opt.dataset.fmt); };
});

function doExport(fmt) {
  if (!fullData) { toast('Nothing to export yet', 'er'); return; }
  switch (fmt) {
    case 'json':
    case 'csv':
    case 'txt':
    case 'pdf':
    case 'docx':
      // Full export — every section in one file, generated server-side in Java
      Api.export(fmt, fullData)
        .then(() => toast('Exported full report (' + fmt.toUpperCase() + ')', 'ok'))
        .catch(e => {
          if (e.status === 401) { window.location.href = '/signin'; return; }
          toast(e.message, 'er');
        });
      break;

    case 'html':
      if (!fullData.html) { toast('No HTML source available', 'er'); break; }
      Util.dlText(fullData.html, 'page-source.html', 'text/html');
      toast('Downloaded HTML source', 'ok'); break;

    case 'shot':
      if (lastUrl) window.open(Util.shotUrl(lastUrl), '_blank', 'noopener');
      break;
  }
}

/* ── per-section export ── */
function sectionSubset(sec) {
  // Metadata is common to every export
  const base = {
    url: fullData.url, title: fullData.title, description: fullData.description,
    keywords: fullData.keywords, language: fullData.language,
    favicon: fullData.favicon, ogImage: fullData.ogImage,
  };
  switch (sec) {
    case 'links':    return { ...base, links: fullData.links, linkCount: fullData.linkCount,
                              internalLinkCount: fullData.internalLinkCount, externalLinkCount: fullData.externalLinkCount };
    case 'images':   return { ...base, images: fullData.images, imageCount: fullData.imageCount };
    case 'headings': return { ...base, headings: fullData.headings, headingCount: fullData.headingCount };
    case 'tables':   return { ...base, tables: fullData.tables };
    case 'contacts': return { ...base, emails: fullData.emails, phones: fullData.phones };
    case 'stats':    return { ...base, stats: fullData.stats };
    default:         return { ...base };
  }
}

function sectionCsvRows(sec) {
  switch (sec) {
    case 'links': {
      const r = [['text', 'href', 'type']];
      (fullData.links || []).forEach(l => r.push([l.text, l.href, l.external ? 'external' : 'internal']));
      return r;
    }
    case 'images': {
      const r = [['alt', 'src']];
      (fullData.images || []).forEach(i => r.push([i.alt, i.src]));
      return r;
    }
    case 'headings': {
      const r = [['tag', 'text']];
      (fullData.headings || []).forEach(h => r.push([h.tag, h.text]));
      return r;
    }
    case 'contacts': {
      const r = [['type', 'value']];
      (fullData.emails || []).forEach(e => r.push(['email', e]));
      (fullData.phones || []).forEach(p => r.push(['phone', p]));
      return r;
    }
    case 'stats': {
      const s = fullData.stats || {};
      return [['metric', 'value'],
        ['words', s.wordCount], ['characters', s.charCount],
        ['paragraphs', s.paragraphCount], ['readingTimeMin', s.readingTimeMin]];
    }
    case 'tables': {
      const r = [];
      (fullData.tables || []).forEach((t, i) => {
        r.push(['Table ' + (i + 1)]);
        if (t.headers) r.push(t.headers);
        t.rows.forEach(row => r.push(row));
        r.push([]);
      });
      return r.length ? r : [['(no tables)']];
    }
    default: return [['(no data)']];
  }
}

function sectionExport(sec, fmt) {
  if (!fullData) { toast('Nothing to export yet', 'er'); return; }
  if (fmt === 'json') {
    Util.dlText(JSON.stringify(sectionSubset(sec), null, 2), sec + '.json', 'application/json');
    toast('Exported ' + sec + ' JSON', 'ok');
  } else if (fmt === 'csv') {
    const rows = sectionCsvRows(sec);
    if (rows.length <= 1) { toast('Nothing to export in ' + sec, 'er'); return; }
    Util.dlCSV(rows, sec + '.csv');
    toast('Exported ' + sec + ' CSV', 'ok');
  } else { // pdf | docx → server-side, scoped to this section
    Api.export(fmt, sectionSubset(sec))
      .then(() => toast('Exported ' + sec + ' ' + fmt.toUpperCase(), 'ok'))
      .catch(e => { if (e.status === 401) { window.location.href = '/signin'; return; } toast(e.message, 'er'); });
  }
}

function tableCsv(i) {
  const t = (fullData.tables || [])[i];
  if (!t) return;
  const rows = [];
  if (t.headers) rows.push(t.headers);
  t.rows.forEach(r => rows.push(r));
  Util.dlCSV(rows, 'table-' + (i + 1) + '.csv');
  toast('Table ' + (i + 1) + ' exported as CSV', 'ok');
}

/* ── init ── */
initAuth();