/* ═══════════════════════════════════════════════
   api.js — all server communication in one place
   ═══════════════════════════════════════════════ */

const API_BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    credentials: 'include',           // send session cookie
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  let data = {};
  try { data = await res.json(); } catch { /* no body */ }
  if (!res.ok || data.error) {
    const err = new Error(data.error || `HTTP ${res.status}`);
    err.status = res.status;
    throw err;
  }
  return data;
}

/* ── Auth ── */
const Api = {
  signup: (name, email, password) =>
    request('/auth/signup', { method: 'POST', body: JSON.stringify({ name, email, password }) }),

  signin: (email, password) =>
    request('/auth/signin', { method: 'POST', body: JSON.stringify({ email, password }) }),

  me: () => request('/auth/me'),

  logout: () => request('/auth/logout', { method: 'POST' }),

  /* ── Scraping ── */
  scrapeFull: (url) =>
    request('/scrape', { method: 'POST', body: JSON.stringify({ url }) }),

  /* ── Server-side export (JSON, CSV, TXT, PDF, DOCX generated in Java) ── */
  async export(format, data) {
    const res = await fetch(API_BASE + '/export/' + format, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = new Error('Export failed (HTTP ' + res.status + ')');
      err.status = res.status;
      throw err;
    }
    const blob = await res.blob();
    const cd = res.headers.get('Content-Disposition') || '';
    const m = cd.match(/filename="?([^"]+)"?/);
    const name = m ? m[1] : ('export.' + format);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = name; a.click();
    URL.revokeObjectURL(url);
  },
};

window.Api = Api;
