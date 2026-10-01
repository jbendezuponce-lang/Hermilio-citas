/* ==========================================================================
   api.js — Cliente REST del backend Spring Boot (JWT).
   Todas las pantallas leen y escriben en MySQL a través de este archivo.
   Para apuntar a otro servidor, cambie API_BASE_URL (o defina localStorage.apiBase).
   ========================================================================== */

const API_BASE_URL = localStorage.getItem('apiBase') || 'http://localhost:8080/api';

class ApiError extends Error {
  constructor(status, message, details) {
    super(message);
    this.status = status;
    this.details = details || [];
  }
}

const Api = {
  async request(method, path, opts = {}) {
    let url = API_BASE_URL + path;
    if (opts.query) {
      const qs = new URLSearchParams();
      Object.entries(opts.query).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.append(k, v); });
      const s = qs.toString();
      if (s) url += '?' + s;
    }
    const headers = {};
    if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
    const token = localStorage.getItem('token');
    if (token) headers['Authorization'] = 'Bearer ' + token;

    let res;
    try {
      res = await fetch(url, { method, headers, body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined });
    } catch (e) {
      throw new ApiError(0, 'No se pudo conectar con el servidor. Verifique que el backend esté encendido (puerto 8080).');
    }
    if (res.status === 204) return null;
    let data = null;
    try { data = await res.json(); } catch (e) { /* respuesta sin cuerpo */ }
    if (!res.ok) {
      if (res.status === 401 && token && !path.startsWith('/auth/')) {
        if (typeof Auth !== 'undefined') Auth.expire();
      }
      let msg = (data && data.message) || ('Error ' + res.status);
      if (data && Array.isArray(data.detalles) && data.detalles.length) msg += ': ' + data.detalles.join(' · ');
      throw new ApiError(res.status, msg, data && data.detalles);
    }
    return data;
  },
  get(path, query) { return this.request('GET', path, { query }); },
  post(path, body) { return this.request('POST', path, { body: body === undefined ? {} : body }); },
  put(path, body) { return this.request('PUT', path, { body }); },
  patch(path, body) { return this.request('PATCH', path, { body }); },
  del(path) { return this.request('DELETE', path); },

  // ----- catálogos
  especialidades: {
    list: () => Api.get('/especialidades'),
    create: (b) => Api.post('/especialidades', b),
    update: (id, b) => Api.put('/especialidades/' + id, b),
    remove: (id) => Api.del('/especialidades/' + id),
  },
  consultorios: {
    list: () => Api.get('/consultorios'),
    create: (b) => Api.post('/consultorios', b),
    update: (id, b) => Api.put('/consultorios/' + id, b),
  },
  medicos: { list: (especialidadId) => Api.get('/medicos', { especialidadId }) },

  // ----- usuarios (administrador)
  usuarios: {
    list: () => Api.get('/usuarios'),
    create: (b) => Api.post('/usuarios', b),
    update: (id, b) => Api.put('/usuarios/' + id, b),
    estado: (id, activo) => Api.patch('/usuarios/' + id + '/estado', { activo }),
  },

  // ----- pacientes
  pacientes: {
    me: () => Api.get('/pacientes/me'),
    updateMe: (b) => Api.put('/pacientes/me', b),
    registrar: (b) => Api.post('/pacientes', b),                 // recepción
    buscar: (documento) => Api.get('/pacientes', { documento }),
  },

  // ----- horarios
  horarios: {
    disponibles: (especialidadId, fecha, turno) => Api.get('/horarios', { especialidadId, fecha, turno }),
    porMedico: (idMedico, desde, hasta) => Api.get('/horarios/medico/' + idMedico, { desde, hasta }),
    create: (b) => Api.post('/horarios', b),
    update: (id, b) => Api.put('/horarios/' + id, b),
    remove: (id) => Api.del('/horarios/' + id),
  },

  // ----- citas
  citas: {
    reservar: (idHorario) => Api.post('/citas', { idHorario }),
    presencial: (idPaciente, idHorario) => Api.post('/citas/presencial', { idPaciente, idHorario }),
    mias: () => Api.get('/citas/mias'),
    agenda: (fecha, medicoId, especialidadId) => Api.get('/citas/agenda', { fecha, medicoId, especialidadId }),
    miAgenda: (fecha) => Api.get('/citas/agenda/mia', { fecha }),
    obtener: (id) => Api.get('/citas/' + id),
    reprogramar: (id, idNuevoHorario) => Api.put('/citas/' + id + '/reprogramar', { idNuevoHorario }),
    cancelar: (id) => Api.patch('/citas/' + id + '/cancelar'),
    checkIn: (id) => Api.patch('/citas/' + id + '/check-in'),
    atender: (id) => Api.patch('/citas/' + id + '/atender'),
    noAsistio: (id) => Api.patch('/citas/' + id + '/no-asistio'),
  },

  // ----- clínico
  historiales: {
    mios: () => Api.get('/historiales/mios'),
    delPaciente: (idPaciente) => Api.get('/historiales/paciente/' + idPaciente),
    registrar: (b) => Api.post('/historiales', b),
    actualizar: (id, b) => Api.put('/historiales/' + id, b),
  },
  triajes: {
    registrar: (b) => Api.post('/triajes', b),
    porCita: (idCita) => Api.get('/triajes/cita/' + idCita),
  },
  listaEspera: {
    list: (especialidadId) => Api.get('/lista-espera', { especialidadId }),
    agregar: (idPaciente, idEspecialidad) => Api.post('/lista-espera', { idPaciente, idEspecialidad }),
    atender: (id) => Api.patch('/lista-espera/' + id + '/atender'),
    remove: (id) => Api.del('/lista-espera/' + id),
  },
  notificaciones: {
    mias: () => Api.get('/notificaciones/mias'),
    leida: (id) => Api.patch('/notificaciones/' + id + '/leida'),
  },

  // ----- reportes y auditoría (administrador)
  reportes: {
    citas: (desde, hasta) => Api.get('/reportes/citas', { desde, hasta }),
    inasistencia: (desde, hasta, especialidadId) => Api.get('/reportes/inasistencia', { desde, hasta, especialidadId }),
    auditoria: (desde, hasta, usuarioId) => Api.get('/auditoria', { desde, hasta, usuarioId }),
  },
};
