/* ═══════════════════════════════════════════════
   render.js — thin client helpers
   (Result HTML is rendered server-side in Java; this
    file only holds small utilities the dashboard needs.)
   ═══════════════════════════════════════════════ */

const $ = (id) => document.getElementById(id);

const Util = {
  validUrl(s) {
    try { const u = new URL(s); return u.protocol === 'http:' || u.protocol === 'https:'; }
    catch { return false; }
  },
  show(id) { $(id).classList.remove('hidden'); },
  hide(id) { $(id).classList.add('hidden'); },
  setLoad(btn, on) { btn.classList.toggle('loading', on); btn.disabled = on; },

  async copy(text) {
    try { await navigator.clipboard.writeText(text); toast('Copied to clipboard', 'ok'); }
    catch { toast('Copy failed', 'er'); }
  },

  dlCSV(rows, name) {
    const csv = rows.map(r => r.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(',')).join('\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    a.download = name; a.click();
  },

  dlText(text, name, mime = 'text/plain') {
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([text], { type: mime }));
    a.download = name; a.click();
  },

  shotUrl(url) {
    return 'https://s.wordpress.com/mshots/v1/' + encodeURIComponent(url) + '?w=1200';
  },

  /* Loads the screenshot into a wrapper that carries data-shot.
     mShots returns a "Generating Preview…" placeholder until the capture is
     ready, so we keep refreshing for a while and offer a manual reload. */
  loadShot(wrap) {
    if (!wrap || wrap.dataset.loaded) return;
    wrap.dataset.loaded = '1';
    const base = wrap.dataset.shot;
    wrap.innerHTML = '<div class="shot-loading"><div class="sp"></div><span>Capturing screenshot… this can take 10–20s while the page is rendered.</span></div>';

    let attempt = 0;
    const max = 6;
    const render = (img) => {
      wrap.innerHTML = '';
      img.className = 'shot-img';
      wrap.appendChild(img);
      const bar = document.createElement('div');
      bar.className = 'shot-actions';
      const open = document.createElement('a');
      open.className = 'btn2'; open.href = img.src; open.target = '_blank'; open.rel = 'noopener';
      open.innerHTML = '<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 14v2a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-2"/><polyline points="7,9 10,12 13,9"/><line x1="10" y1="3" x2="10" y2="12"/></svg> Open full image';
      const reload = document.createElement('button');
      reload.className = 'btn2';
      reload.innerHTML = '<svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 10a7 7 0 0 1 12-5l2 2M17 10a7 7 0 0 1-12 5l-2-2"/><path d="M17 3v4h-4M3 17v-4h4"/></svg> Reload';
      reload.onclick = () => { wrap.dataset.loaded = ''; Util.loadShot(wrap); };
      bar.appendChild(open); bar.appendChild(reload);
      wrap.appendChild(bar);
    };

    const poll = () => {
      attempt++;
      const img = new Image();
      img.alt = 'Page screenshot';
      img.onload = () => {
        render(img);
        // keep refreshing toward the final render (mShots replaces the placeholder)
        if (attempt < max) setTimeout(poll, 4000);
      };
      img.onerror = () => {
        if (attempt < max) { setTimeout(poll, 4000); return; }
        wrap.innerHTML = '<div class="empty"><span>Screenshot unavailable. '
          + '<a href="' + base + '" target="_blank" rel="noopener" style="color:var(--primary-l)">Try opening it directly.</a></span></div>';
      };
      img.src = base + '&r=' + Date.now() + '_' + attempt;
    };
    // small initial delay gives mShots a head start before the first display
    setTimeout(poll, 2500);
  },
};

let _toastTimer;
function toast(msg, type = '') {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'toast' + (type ? ' ' + type : '');
  void t.offsetWidth;
  t.classList.add('show');
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => t.classList.remove('show'), 3000);
}

window.$ = $;
window.Util = Util;
window.toast = toast;
