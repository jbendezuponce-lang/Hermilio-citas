/* ==========================================================================
   auth.js — Autenticación real contra el backend Spring Boot (JWT)
   Requiere js/api.js (define API_BASE_URL y Api) cargado antes.
   ========================================================================== */

const Auth = {
  async login(email, password) {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ correo: email, password: password })
      });
      if (!response.ok) {
        const err = await readError(response);
        if (response.status === 401) return { ok: false, message: 'Correo o contraseña inválidos, o la cuenta está inactiva.' };
        return { ok: false, message: err.message || ('Error del servidor (' + response.status + ').') };
      }
      const data = await response.json();
      localStorage.setItem('token', data.token);
      localStorage.setItem('token_exp', String(Date.now() + (data.expiraEnSegundos || 3600) * 1000));

      const backendRole = data.rol || 'PACIENTE';
      let frontendRole = 'patient';
      if (backendRole.includes('ADMINISTRADOR')) frontendRole = 'admin';
      else if (backendRole.includes('MEDICO')) frontendRole = 'doctor';
      else if (backendRole.includes('RECEPCIONISTA')) frontendRole = 'receptionist';

      const session = {
        email: email,
        role: frontendRole,
        rol: backendRole,
        name: ((data.nombres || '') + ' ' + (data.apellidos || '')).trim() || 'Usuario',
        id: data.idUsuario
      };
      localStorage.setItem('session', JSON.stringify(session));
      return { ok: true, user: session };
    } catch (error) {
      console.error('Error de conexión:', error);
      return { ok: false, message: 'El servidor backend no está respondiendo.' };
    }
  },

  logout() {
    // Invalida el token en el servidor (RF-15) sin bloquear la salida
    const token = localStorage.getItem('token');
    if (token) {
      try { fetch(`${API_BASE_URL}/auth/logout`, { method: 'POST', headers: { Authorization: 'Bearer ' + token }, keepalive: true }); } catch (e) { /* ignorar */ }
    }
    this.clear();
    window.location.href = getBasePath() + 'pages/auth/login.html';
  },

  clear() {
    ['session', 'token', 'token_exp'].forEach(k => localStorage.removeItem(k));
  },

  /** Sesión vencida o token rechazado por el servidor. */
  expire() {
    this.clear();
    window.location.href = getBasePath() + 'pages/auth/login.html?expired=1';
  },

  tokenVigente() {
    const t = localStorage.getItem('token');
    const exp = parseInt(localStorage.getItem('token_exp') || '0', 10);
    return !!t && Date.now() < exp;
  },

  currentUser() {
    if (!this.tokenVigente()) return null;
    try { return JSON.parse(localStorage.getItem('session')); } catch (e) { return null; }
  },

  isLoggedIn() { return !!this.currentUser(); },

  requireRole(roles) {
    const user = this.currentUser();
    const basePath = getBasePath();
    if (!user) {
      this.clear();
      window.location.href = basePath + 'pages/auth/login.html';
      return null;
    }
    if (roles && !roles.includes(user.role)) {
      window.location.href = basePath + 'pages/errors/403.html';
      return null;
    }
    return user;
  },

  redirectIfLoggedIn() {
    const user = this.currentUser();
    if (user) window.location.href = dashboardFor(user.role);
  },

  async register(patientData) {
    // Los nombres de los campos deben coincidir con RegistroPacienteRequest.java
    try {
      const response = await fetch(`${API_BASE_URL}/auth/registro`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          documentoIdentidad: (patientData.nationalId || '').trim(),
          nombres: (patientData.firstName || '').trim(),
          apellidos: (patientData.lastName || '').trim(),
          correo: (patientData.email || '').trim().toLowerCase(),
          password: patientData.password,
          telefonoContacto: (patientData.phone || '').trim()
        })
      });
      if (!response.ok) {
        const err = await readError(response);
        let msg = err.message || 'Error al registrar.';
        if (Array.isArray(err.detalles) && err.detalles.length) msg += ' ' + err.detalles.join(' | ');
        return { ok: false, message: msg };
      }
      return { ok: true };
    } catch (error) {
      console.error('Error de conexión:', error);
      return { ok: false, message: 'El servidor backend no está respondiendo.' };
    }
  }
};

/** Lee el cuerpo de error (ApiError) sin romper si la respuesta no es JSON. */
async function readError(response) {
  try { return await response.json(); } catch (e) { return {}; }
}

function dashboardFor(role) {
  const basePath = getBasePath();
  switch (role) {
    case 'patient': return basePath + 'pages/patient/dashboard.html';
    case 'doctor': return basePath + 'pages/doctor/dashboard.html';
    case 'receptionist': return basePath + 'pages/receptionist/dashboard.html';
    case 'admin': return basePath + 'pages/admin/dashboard.html';
    default: return basePath + 'index.html';
  }
}

function getBasePath() {
  const path = window.location.pathname;
  const depth = (path.split('/pages/')[1] || '').split('/').length - 1;
  if (!path.includes('/pages/')) return '';
  return '../'.repeat(depth + 1);
}
