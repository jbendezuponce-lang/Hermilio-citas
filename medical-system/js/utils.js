/* ==========================================================================
   utils.js — Toasts, validation helpers, badges, misc utilities
   ========================================================================== */

function toast(message, type = 'success', duration = 3500) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const icons = { success: 'fa-circle-check', error: 'fa-circle-exclamation', info: 'fa-circle-info', warning: 'fa-triangle-exclamation' };
  const el = document.createElement('div');
  el.className = `toast-item toast-${type}`;
  el.innerHTML = `<i class="fa-solid ${icons[type] || icons.info}"></i><span>${esc(message)}</span><button class="toast-close" aria-label="Close">&times;</button>`;
  container.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  const remove = () => { el.classList.remove('show'); setTimeout(() => el.remove(), 250); };
  el.querySelector('.toast-close').addEventListener('click', remove);
  setTimeout(remove, duration);
}

function statusBadge(status) {
  const map = {
    'Confirmed': 'badge-blue', 'Pending': 'badge-amber', 'Completed': 'badge-green',
    'Cancelled': 'badge-red', 'Waiting': 'badge-purple', 'In Consultation': 'badge-teal',
    'Active': 'badge-green', 'Inactive': 'badge-gray', 'Available': 'badge-green', 'Success': 'badge-green'
  };
  return `<span class="badge ${map[status] || 'badge-gray'}">${status}</span>`;
}

function initials(name) {
  return (name || '').split(' ').filter(Boolean).slice(0, 2).map(n => n[0]).join('').toUpperCase();
}

function isValidEmail(v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v); }
function isValidPhone(v) { return /^[+]?[\d\s()-]{7,20}$/.test(v); }

function passwordStrength(pw) {
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  if (pw.length >= 12) score++;
  return score; // 0-5
}

function showFieldError(input, message) {
  clearFieldError(input);
  input.classList.add('is-invalid');
  const err = document.createElement('div');
  err.className = 'field-error';
  err.textContent = message;
  input.closest('.form-group')?.appendChild(err);
}
function clearFieldError(input) {
  input.classList.remove('is-invalid');
  const group = input.closest('.form-group');
  const err = group?.querySelector('.field-error');
  if (err) err.remove();
}
function clearAllFieldErrors(form) {
  form.querySelectorAll('.is-invalid').forEach(clearFieldError);
}

function confirmDialog(message, onConfirm, opts = {}) {
  const existing = document.getElementById('confirm-modal-overlay');
  if (existing) existing.remove();
  const overlay = document.createElement('div');
  overlay.id = 'confirm-modal-overlay';
  overlay.className = 'modal-overlay show';
  overlay.innerHTML = `
    <div class="modal-box modal-sm">
      <div class="modal-header">
        <h3>${opts.title || 'Please confirm'}</h3>
      </div>
      <div class="modal-body"><p>${message}</p></div>
      <div class="modal-footer">
        <button class="btn btn-outline" id="confirm-cancel">Cancel</button>
        <button class="btn ${opts.danger ? 'btn-danger' : 'btn-primary'}" id="confirm-ok">${opts.confirmText || 'Confirm'}</button>
      </div>
    </div>`;
  document.body.appendChild(overlay);
  overlay.querySelector('#confirm-cancel').addEventListener('click', () => overlay.remove());
  overlay.addEventListener('click', (e) => { if (e.target === overlay) overlay.remove(); });
  overlay.querySelector('#confirm-ok').addEventListener('click', () => { overlay.remove(); onConfirm(); });
}

function openModal(id) { document.getElementById(id)?.classList.add('show'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('show'); }

function debounce(fn, wait = 250) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), wait); };
}

function qs(sel, ctx = document) { return ctx.querySelector(sel); }
function qsa(sel, ctx = document) { return [...ctx.querySelectorAll(sel)]; }

function emptyState(container, icon, title, message) {
  container.innerHTML = `
    <div class="empty-state">
      <i class="fa-solid ${icon}"></i>
      <h4>${title}</h4>
      <p>${message}</p>
    </div>`;
}

function skeletonRows(container, rows = 4, cols = 5) {
  let html = '';
  for (let r = 0; r < rows; r++) {
    html += '<tr>';
    for (let c = 0; c < cols; c++) html += '<td><div class="skeleton"></div></td>';
    html += '</tr>';
  }
  container.innerHTML = html;
}


/* ---------- API-era helpers ---------- */
function esc(v) {
  return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function fmtTime(t) { return t ? String(t).slice(0, 5) : '—'; }
function formatDateTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
const ESTADO_CITA = {
  CONFIRMADA: { label: 'Confirmed', cls: 'badge-blue' },
  REPROGRAMADA: { label: 'Rescheduled', cls: 'badge-purple' },
  CHECK_IN: { label: 'Checked in', cls: 'badge-teal' },
  ATENDIDA: { label: 'Completed', cls: 'badge-green' },
  CANCELADA: { label: 'Cancelled', cls: 'badge-red' },
  NO_ASISTIO: { label: 'No-show', cls: 'badge-gray' }
};
function estadoBadge(e) {
  const x = ESTADO_CITA[e] || { label: e, cls: 'badge-gray' };
  return `<span class="badge ${x.cls}">${x.label}</span>`;
}
function estadoLabel(e) { return (ESTADO_CITA[e] || { label: e }).label; }
const ROL_LABEL = { PACIENTE: 'Patient', RECEPCIONISTA: 'Receptionist', MEDICO: 'Doctor', ADMINISTRADOR: 'Administrator' };
const ROL_BADGE = { PACIENTE: 'badge-green', RECEPCIONISTA: 'badge-amber', MEDICO: 'badge-teal', ADMINISTRADOR: 'badge-purple' };
function rolBadge(r) { return `<span class="badge ${ROL_BADGE[r] || 'badge-gray'}">${ROL_LABEL[r] || r}</span>`; }
function activoBadge(a) { return `<span class="badge ${a ? 'badge-green' : 'badge-gray'}">${a ? 'Active' : 'Inactive'}</span>`; }
function fullName(o) { return ((o.nombres || '') + ' ' + (o.apellidos || '')).trim(); }
function fail(e) {
  console.error(e);
  toast(e && e.message ? e.message : 'Unexpected error', 'error', 6500);
}
async function safe(fn) { try { return await fn(); } catch (e) { fail(e); return undefined; } }
function setLoading(container) {
  container.innerHTML = '<div class="empty-state"><i class="fa-solid fa-spinner fa-spin"></i><h4>Loading…</h4></div>';
}
function toCsv(rows) {
  return rows.map(r => r.map(c => '"' + String(c ?? '').replace(/"/g, '""') + '"').join(',')).join('\n');
}
function downloadFile(name, text, type = 'text/csv') {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob(['\ufeff' + text], { type: type + ';charset=utf-8' }));
  a.download = name; document.body.appendChild(a); a.click(); a.remove();
}
function qparam(name) { return new URLSearchParams(location.search).get(name); }
