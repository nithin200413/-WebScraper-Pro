/* ═══════════════════════════════════════════════
   signup.js — sign-up page controller
   Depends on: api.js
   ═══════════════════════════════════════════════ */

const $a = (id) => document.getElementById(id);

/* theme */
const htmlEl = document.documentElement;
htmlEl.setAttribute('data-theme', localStorage.getItem('theme') || 'dark');
$a('themeBtn').onclick = () => {
  const n = htmlEl.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  htmlEl.setAttribute('data-theme', n);
  localStorage.setItem('theme', n);
};

/* already logged in? → dashboard */
(async () => {
  try { await Api.me(); window.location.href = '/dashboard'; } catch {}
})();

/* eye toggles */
document.querySelectorAll('.eye-btn').forEach(btn => {
  btn.onclick = () => {
    const input = $a(btn.dataset.target);
    const reveal = input.type === 'password';
    input.type = reveal ? 'text' : 'password';
    btn.classList.toggle('revealed', reveal);
    btn.setAttribute('aria-label', reveal ? 'Hide password' : 'Show password');
  };
});

async function submit() {
  const name    = $a('nameIn').value.trim();
  const email   = $a('emailIn').value.trim();
  const pass    = $a('passIn').value;
  const confirm = $a('confirmIn').value;
  $a('authErr').textContent = '';
  clearErrors();

  if (name.length < 2) { fail('nameIn', 'Name must be at least 2 characters.'); return; }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) { fail('emailIn', 'Enter a valid email address.'); return; }
  if (pass.length < 6) { fail('passIn', 'Password must be at least 6 characters.'); return; }
  if (pass !== confirm) { fail('confirmIn', 'Passwords do not match.'); $a('passIn').classList.add('error'); return; }

  const btn = $a('submitBtn');
  btn.classList.add('loading'); btn.disabled = true;
  try {
    await Api.signup(name, email, pass);
    window.location.href = '/dashboard';
  } catch (e) {
    $a('authErr').textContent = e.message;
    $a('emailIn').classList.add('error');
  } finally {
    btn.classList.remove('loading'); btn.disabled = false;
  }
}

const FIELDS = ['nameIn', 'emailIn', 'passIn', 'confirmIn'];
function fail(id, msg) { $a('authErr').textContent = msg; $a(id).classList.add('error'); $a(id).focus(); }
function clearErrors() { FIELDS.forEach(id => $a(id).classList.remove('error')); }
FIELDS.forEach(id => $a(id).addEventListener('input', () => $a(id).classList.remove('error')));

$a('submitBtn').onclick = submit;
FIELDS.forEach(id => $a(id).addEventListener('keydown', e => { if (e.key === 'Enter') submit(); }));
