/* ==========================================================================
   auth.js — Real Authentication connecting to Spring Boot Backend
   ========================================================================== */

const API_BASE_URL = 'http://localhost:8080/api';

const Auth = {
  async login(email, password) {
    try {
      // 1. Petición al backend enviando correo y password
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ correo: email, password: password }) // Coincide con tu AuthDto.java
      });

      if (!response.ok) {
        const err = await readError(response);
        if (response.status === 401) {
          return { ok: false, message: 'Correo o contraseña inválidos, o la cuenta está inactiva.' };
        }
        return { ok: false, message: err.message || ('Error del servidor (' + response.status + ').') };
      }

      // 2. Extraer respuesta (el backend debe devolver el token JWT y datos del usuario)
      const data = await response.json();
      
      // Guardamos el token en localStorage para enviarlo en futuras peticiones
      localStorage.setItem('token', data.token);

      // 3. Mapear los roles del Backend (Español) a los roles del Frontend (Inglés) para las rutas
      let frontendRole = 'patient';
      // Si tu backend devuelve "ROLE_ADMINISTRADOR" o "ADMINISTRADOR", lo adaptamos:
      const backendRole = data.rol || 'PACIENTE'; 
      if (backendRole.includes('ADMINISTRADOR')) frontendRole = 'admin';
      else if (backendRole.includes('MEDICO')) frontendRole = 'doctor';
      else if (backendRole.includes('RECEPCIONISTA')) frontendRole = 'receptionist';

      const session = {
        email: email,
        role: frontendRole,
        name: data.nombres || 'Usuario', // Ajusta según tu DTO
        id: data.idUsuario
      };
      
      // Guardar la sesión para mantener la compatibilidad con tu HTML
      localStorage.setItem('session', JSON.stringify(session));
      return { ok: true, user: session };
      
    } catch (error) {
      console.error("Error de conexión:", error);
      return { ok: false, message: 'El servidor backend no está respondiendo.' };
    }
  },

  logout() {
    localStorage.removeItem('session');
    localStorage.removeItem('token'); // Destruimos el JWT del navegador
    window.location.href = getBasePath() + 'pages/auth/login.html';
  },

  currentUser() {
    try { 
      return JSON.parse(localStorage.getItem('session')); 
    } catch (e) { 
      return null; 
    }
  },

  isLoggedIn() {
    return !!this.currentUser() && !!localStorage.getItem('token');
  },

  requireRole(roles) {
    const user = this.currentUser();
    const basePath = getBasePath();
    
    if (!user) {
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
    // Los nombres de los campos deben coincidir EXACTAMENTE con RegistroPacienteRequest.java (camelCase).
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
        if (Array.isArray(err.detalles) && err.detalles.length) {
          msg += ' ' + err.detalles.join(' | ');
        }
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