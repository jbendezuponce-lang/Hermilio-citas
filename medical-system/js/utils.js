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
  el.innerHTML = `<i class="fa-solid ${icons[type] || icons.info}"></i><span>${message}</span><button class="toast-close" aria-label="Close">&times;</button>`;
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
