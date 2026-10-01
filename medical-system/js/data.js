/* ==========================================================================
   data.js — Mock data layer for MediCare+ frontend
   Seeds localStorage on first load. All "backend" behavior in this app is
   simulated entirely in the browser using localStorage.
   ========================================================================== */

const DB_KEYS = {
  users: 'mc_users',
  patients: 'mc_patients',
  doctors: 'mc_doctors',
  specialties: 'mc_specialties',
  locations: 'mc_locations',
  appointments: 'mc_appointments',
  consultations: 'mc_consultations',
  documents: 'mc_documents',
  notifications: 'mc_notifications',
  auditLogs: 'mc_audit_logs',
  session: 'mc_current_user',
  seeded: 'mc_seeded_v1'
};

const SPECIALTIES_SEED = [
  { id: 'sp1', name: 'Cardiology', icon: 'fa-heart-pulse', description: 'Diagnosis and treatment of heart and cardiovascular conditions.', doctorCount: 3, status: 'Active' },
  { id: 'sp2', name: 'Dermatology', icon: 'fa-hand-dots', description: 'Skin, hair and nail health for all ages.', doctorCount: 2, status: 'Active' },
  { id: 'sp3', name: 'Pediatrics', icon: 'fa-child-reaching', description: 'Comprehensive medical care for infants, children and teens.', doctorCount: 2, status: 'Active' },
  { id: 'sp4', name: 'Neurology', icon: 'fa-brain', description: 'Disorders of the brain, spine and nervous system.', doctorCount: 2, status: 'Active' },
  { id: 'sp5', name: 'Dentistry', icon: 'fa-tooth', description: 'Preventive, restorative and cosmetic dental care.', doctorCount: 2, status: 'Active' },
  { id: 'sp6', name: 'Traumatology', icon: 'fa-bone', description: 'Bone, joint and muscle injuries and rehabilitation.', doctorCount: 1, status: 'Active' },
  { id: 'sp7', name: 'General Medicine', icon: 'fa-stethoscope', description: 'Primary care and general health checkups.', doctorCount: 3, status: 'Active' },
  { id: 'sp8', name: 'Gynecology', icon: 'fa-venus', description: 'Women\'s reproductive and overall health.', doctorCount: 2, status: 'Active' },
  { id: 'sp9', name: 'Ophthalmology', icon: 'fa-eye', description: 'Eye care, vision correction and eye surgery.', doctorCount: 1, status: 'Active' },
  { id: 'sp10', name: 'Psychiatry', icon: 'fa-brain', description: 'Mental health diagnosis, therapy and treatment.', doctorCount: 1, status: 'Active' }
];

const LOCATIONS_SEED = [
  { id: 'loc1', name: 'MediCare+ Central Clinic', address: '120 Wellness Ave, Downtown', phone: '+1 (555) 010-2200', rooms: ['Room 101', 'Room 102', 'Room 103'] },
  { id: 'loc2', name: 'MediCare+ North Medical Center', address: '48 Sunrise Blvd, Northside', phone: '+1 (555) 010-2299', rooms: ['Room 201', 'Room 202'] },
  { id: 'loc3', name: 'MediCare+ Virtual Care', address: 'Online / Telemedicine', phone: '+1 (555) 010-2255', rooms: ['Virtual Room A', 'Virtual Room B'] }
];

const DOCTOR_NAMES = [
  { name: 'Dr. Sarah Bennett', spec: 'sp1', photo: 'https://i.pravatar.cc/300?img=47' },
  { name: 'Dr. Michael Chen', spec: 'sp1', photo: 'https://i.pravatar.cc/300?img=12' },
  { name: 'Dr. Laura Gomez', spec: 'sp1', photo: 'https://i.pravatar.cc/300?img=32' },
  { name: 'Dr. James Whitfield', spec: 'sp2', photo: 'https://i.pravatar.cc/300?img=13' },
  { name: 'Dr. Amara Okafor', spec: 'sp2', photo: 'https://i.pravatar.cc/300?img=25' },
  { name: 'Dr. Emily Torres', spec: 'sp3', photo: 'https://i.pravatar.cc/300?img=48' },
  { name: 'Dr. Daniel Kim', spec: 'sp3', photo: 'https://i.pravatar.cc/300?img=14' },
  { name: 'Dr. Priya Sharma', spec: 'sp4', photo: 'https://i.pravatar.cc/300?img=45' },
  { name: 'Dr. Robert Hayes', spec: 'sp4', photo: 'https://i.pravatar.cc/300?img=15' },
  { name: 'Dr. Sofia Marino', spec: 'sp5', photo: 'https://i.pravatar.cc/300?img=44' },
  { name: 'Dr. Thomas Reed', spec: 'sp5', photo: 'https://i.pravatar.cc/300?img=16' },
  { name: 'Dr. Nina Petrova', spec: 'sp6', photo: 'https://i.pravatar.cc/300?img=43' },
  { name: 'Dr. Carlos Mendes', spec: 'sp7', photo: 'https://i.pravatar.cc/300?img=17' },
  { name: 'Dr. Grace Anderson', spec: 'sp7', photo: 'https://i.pravatar.cc/300?img=41' },
  { name: 'Dr. Omar Farouk', spec: 'sp7', photo: 'https://i.pravatar.cc/300?img=18' },
  { name: 'Dr. Elena Volkov', spec: 'sp8', photo: 'https://i.pravatar.cc/300?img=40' },
  { name: 'Dr. Rachel Adams', spec: 'sp8', photo: 'https://i.pravatar.cc/300?img=39' },
  { name: 'Dr. Kevin Park', spec: 'sp9', photo: 'https://i.pravatar.cc/300?img=19' },
  { name: 'Dr. Hannah Wells', spec: 'sp10', photo: 'https://i.pravatar.cc/300?img=38' }
];

function seedDatabase() {
  if (localStorage.getItem(DB_KEYS.seeded)) return;

  // Specialties & Locations
  localStorage.setItem(DB_KEYS.specialties, JSON.stringify(SPECIALTIES_SEED));
  localStorage.setItem(DB_KEYS.locations, JSON.stringify(LOCATIONS_SEED));

  // Users (all roles) + role-specific profiles
  const users = [];
  const doctors = [];
  const patients = [];

  // Fixed demo accounts
  users.push({ id: 'u-admin', name: 'Alex Morgan', email: 'admin@example.com', password: 'Admin123!', role: 'admin', status: 'Active', registered: '2024-01-10', lastActivity: today() });
  users.push({ id: 'u-recep', name: 'Jordan Blake', email: 'reception@example.com', password: 'Reception123!', role: 'receptionist', status: 'Active', registered: '2024-02-14', lastActivity: today() });
  users.push({ id: 'u-doctor', name: 'Dr. Sarah Bennett', email: 'doctor@example.com', password: 'Doctor123!', role: 'doctor', status: 'Active', registered: '2023-11-02', lastActivity: today(), doctorId: 'doc1' });
  users.push({ id: 'u-patient', name: 'John Carter', email: 'patient@example.com', password: 'Patient123!', role: 'patient', status: 'Active', registered: '2024-03-20', lastActivity: today(), patientId: 'pat1' });

  // Generate doctor profiles
  DOCTOR_NAMES.forEach((d, i) => {
    const id = 'doc' + (i + 1);
    doctors.push({
      id,
      userId: i === 0 ? 'u-doctor' : null,
      name: d.name,
      specialtyId: d.spec,
      photo: d.photo,
      experience: 4 + (i % 15),
      rating: (3.9 + (i % 10) * 0.1).toFixed(1),
      education: 'MD, State University School of Medicine',
      certifications: ['Board Certified', 'Advanced Life Support'],
      bio: `${d.name} is a dedicated physician with a strong focus on patient-centered care, combining clinical expertise with compassionate communication.`,
      consultationTypes: i % 3 === 0 ? ['In person', 'Virtual'] : (i % 3 === 1 ? ['In person'] : ['Virtual']),
      locationId: LOCATIONS_SEED[i % LOCATIONS_SEED.length].id,
      professionalId: 'MED-' + (10234 + i),
      phone: '+1 (555) 02' + (10 + i) + '-00' + (i + 10),
      email: d.name.toLowerCase().replace(/[^a-z]+/g, '.') + '@medicareplus.com',
      status: 'Active',
      schedule: [
        { day: 'Monday', from: '08:00', to: '13:00' },
        { day: 'Wednesday', from: '08:00', to: '13:00' },
        { day: 'Friday', from: '14:00', to: '18:00' }
      ],
      blockedDates: []
    });
  });

  // Generate patient profiles
  const patientNames = [
    'John Carter', 'Maria Lopez', 'David Kim', 'Grace Owusu', 'Liam Johnson',
    'Ava Martinez', 'Noah Williams', 'Isabella Rossi', 'Ethan Brown', 'Mia Chen'
  ];
  patientNames.forEach((name, i) => {
    const id = 'pat' + (i + 1);
    patients.push({
      id,
      userId: i === 0 ? 'u-patient' : null,
      name,
      nationalId: '9' + (10000000 + i * 137),
      dob: `19${80 + (i % 20)}-0${(i % 9) + 1}-1${i % 9}`,
      gender: i % 2 === 0 ? 'Male' : 'Female',
      phone: '+1 (555) 03' + (10 + i) + '-0' + (100 + i),
      email: name.toLowerCase().replace(/[^a-z]+/g, '.') + '@mail.com',
      address: `${100 + i} Maple Street, Springfield`,
      photo: `https://i.pravatar.cc/300?img=${60 + i}`,
      registered: `2024-0${(i % 9) + 1}-1${i % 9}`,
      status: 'Active',
      allergies: i % 3 === 0 ? ['Penicillin'] : (i % 3 === 1 ? ['None known'] : ['Pollen', 'Dust']),
      bloodType: ['O+', 'A+', 'B+', 'AB+', 'O-'][i % 5]
    });
  });

  users.push(...patients.filter(p => !p.userId).map((p, i) => ({
    id: 'u-pat-' + p.id, name: p.name, email: p.email, password: 'Patient123!',
    role: 'patient', status: 'Active', registered: p.registered, lastActivity: today(), patientId: p.id
  })));
  users.push(...doctors.filter(d => !d.userId).map((d, i) => ({
    id: 'u-doc-' + d.id, name: d.name, email: d.email, password: 'Doctor123!',
    role: 'doctor', status: 'Active', registered: '2023-11-0' + ((i % 8) + 1), lastActivity: today(), doctorId: d.id
  })));

  // Appointments (mix of statuses across dates)
  const appointments = [];
  const statuses = ['Confirmed', 'Pending', 'Completed', 'Cancelled'];
  let apptCounter = 1000;
  for (let i = 0; i < 26; i++) {
    const doc = doctors[i % doctors.length];
    const pat = patients[i % patients.length];
    const dayOffset = (i % 13) - 6; // spread across past & future
    const status = i < 4 ? 'Confirmed' : statuses[i % statuses.length];
    appointments.push({
      id: 'APT-' + (apptCounter++),
      patientId: pat.id,
      doctorId: doc.id,
      specialtyId: doc.specialtyId,
      date: addDays(dayOffset),
      time: ['09:00', '09:30', '10:00', '10:30', '11:00', '14:00', '14:30', '15:00'][i % 8],
      type: doc.consultationTypes[i % doc.consultationTypes.length],
      locationId: doc.locationId,
      reason: ['Routine checkup', 'Follow-up visit', 'New symptoms evaluation', 'Prescription renewal', 'Annual physical'][i % 5],
      status,
      createdAt: addDays(dayOffset - 3)
    });
  }
  // Ensure demo patient (pat1) & demo doctor (doc1) have visible data today/future
  appointments.push({ id: 'APT-2001', patientId: 'pat1', doctorId: 'doc1', specialtyId: 'sp1', date: addDays(2), time: '10:00', type: 'In person', locationId: 'loc1', reason: 'Chest pain follow-up', status: 'Confirmed', createdAt: addDays(-2) });
  appointments.push({ id: 'APT-2002', patientId: 'pat1', doctorId: 'doc7', specialtyId: 'sp4', date: addDays(5), time: '14:30', type: 'Virtual', locationId: 'loc3', reason: 'Migraine consultation', status: 'Pending', createdAt: addDays(-1) });
  appointments.push({ id: 'APT-2003', patientId: 'pat2', doctorId: 'doc1', specialtyId: 'sp1', date: today(), time: '09:00', type: 'In person', locationId: 'loc1', reason: 'Blood pressure check', status: 'Confirmed', createdAt: addDays(-4) });
  appointments.push({ id: 'APT-2004', patientId: 'pat3', doctorId: 'doc1', specialtyId: 'sp1', date: today(), time: '09:30', type: 'In person', locationId: 'loc1', reason: 'Post-op review', status: 'Confirmed', createdAt: addDays(-6) });
  appointments.push({ id: 'APT-2005', patientId: 'pat1', doctorId: 'doc1', specialtyId: 'sp1', date: addDays(-10), time: '11:00', type: 'In person', locationId: 'loc1', reason: 'Annual physical', status: 'Completed', createdAt: addDays(-15) });

  // Consultations (linked to completed appointments)
  const consultations = [
    {
      id: 'CON-500', patientId: 'pat1', doctorId: 'doc1', specialtyId: 'sp1', date: addDays(-10),
      reason: 'Annual physical', symptoms: 'None reported, routine visit',
      vitals: { weight: '78 kg', height: '176 cm', bp: '122/79', temp: '36.6°C', hr: '72 bpm', spo2: '98%' },
      diagnosis: 'Overall good health. Mild borderline cholesterol.',
      treatment: 'Dietary adjustment recommended, re-check lipid panel in 6 months.',
      medications: ['Atorvastatin 10mg — once daily (if levels persist)'],
      recommendations: 'Increase physical activity to 150 min/week, reduce saturated fat intake.',
      notes: 'Patient in good spirits, no acute concerns.',
      followUp: addDays(170)
    },
    {
      id: 'CON-501', patientId: 'pat2', doctorId: 'doc4', specialtyId: 'sp2', date: addDays(-30),
      reason: 'Skin rash', symptoms: 'Itchy red patches on forearm',
      vitals: { weight: '65 kg', height: '162 cm', bp: '118/76', temp: '36.7°C', hr: '80 bpm', spo2: '99%' },
      diagnosis: 'Contact dermatitis',
      treatment: 'Topical corticosteroid cream, avoid known irritant.',
      medications: ['Hydrocortisone 1% cream — twice daily for 7 days'],
      recommendations: 'Use fragrance-free soap and moisturizer.',
      notes: 'Follow up only if symptoms persist beyond 2 weeks.',
      followUp: null
    },
    {
      id: 'CON-502', patientId: 'pat3', doctorId: 'doc1', specialtyId: 'sp1', date: addDays(-60),
      reason: 'Chest tightness', symptoms: 'Occasional tightness during exercise',
      vitals: { weight: '82 kg', height: '178 cm', bp: '134/88', temp: '36.5°C', hr: '76 bpm', spo2: '97%' },
      diagnosis: 'Mild exertional angina, suspected',
      treatment: 'Stress test ordered, lifestyle modification advised.',
      medications: ['Aspirin 81mg — once daily'],
      recommendations: 'Reduce sodium intake, schedule cardiac stress test.',
      notes: 'Patient advised to seek immediate care if symptoms worsen.',
      followUp: addDays(-30)
    }
  ];

  // Documents
  const documents = [
    { id: 'DOC-1', patientId: 'pat1', name: 'Lipid Panel Results.pdf', type: 'Laboratory results', date: addDays(-10), doctorId: 'doc1', size: '214 KB', status: 'Available' },
    { id: 'DOC-2', patientId: 'pat1', name: 'Atorvastatin Prescription.pdf', type: 'Prescriptions', date: addDays(-10), doctorId: 'doc1', size: '88 KB', status: 'Available' },
    { id: 'DOC-3', patientId: 'pat1', name: 'Annual Physical Report.pdf', type: 'Medical reports', date: addDays(-10), doctorId: 'doc1', size: '340 KB', status: 'Available' },
    { id: 'DOC-4', patientId: 'pat1', name: 'Chest X-Ray.pdf', type: 'Imaging', date: addDays(-180), doctorId: 'doc1', size: '1.2 MB', status: 'Available' },
    { id: 'DOC-5', patientId: 'pat1', name: 'Fitness Clearance Certificate.pdf', type: 'Certificates', date: addDays(-180), doctorId: 'doc7', size: '76 KB', status: 'Available' },
    { id: 'DOC-6', patientId: 'pat2', name: 'Dermatology Report.pdf', type: 'Medical reports', date: addDays(-30), doctorId: 'doc4', size: '120 KB', status: 'Available' }
  ];

  // Notifications
  const notifications = [
    { id: 'N-1', userId: 'u-patient', title: 'Upcoming appointment', message: 'Your appointment with Dr. Sarah Bennett is in 2 days.', type: 'appointment', date: addDays(0), read: false },
    { id: 'N-2', userId: 'u-patient', title: 'Appointment confirmed', message: 'Your appointment for Migraine consultation is now pending doctor confirmation.', type: 'appointment', date: addDays(-1), read: false },
    { id: 'N-3', userId: 'u-patient', title: 'New medical document', message: 'Dr. Sarah Bennett uploaded "Lipid Panel Results.pdf" to your records.', type: 'document', date: addDays(-10), read: true },
    { id: 'N-4', userId: 'u-doctor', title: "Today's schedule ready", message: 'You have 2 confirmed appointments today.', type: 'schedule', date: addDays(0), read: false },
    { id: 'N-5', userId: 'u-recep', title: 'New appointment booked', message: 'John Carter booked an appointment with Dr. Priya Sharma.', type: 'appointment', date: addDays(-1), read: false },
    { id: 'N-6', userId: 'u-admin', title: 'System report ready', message: 'Monthly appointments report has been generated.', type: 'system', date: addDays(-2), read: true }
  ];

  // Audit logs
  const auditLogs = [
    { id: 'AL-1', user: 'John Carter', action: 'Booked appointment', module: 'Appointments', date: addDays(-2), time: '10:14', status: 'Success' },
    { id: 'AL-2', user: 'Dr. Sarah Bennett', action: 'Registered consultation', module: 'Medical Records', date: addDays(-10), time: '11:02', status: 'Success' },
    { id: 'AL-3', user: 'Alex Morgan', action: 'Created doctor account', module: 'Users', date: addDays(-20), time: '09:41', status: 'Success' },
    { id: 'AL-4', user: 'Jordan Blake', action: 'Cancelled appointment', module: 'Appointments', date: addDays(-3), time: '15:30', status: 'Success' },
    { id: 'AL-5', user: 'Maria Lopez', action: 'Updated profile information', module: 'Patients', date: addDays(-5), time: '08:22', status: 'Success' },
    { id: 'AL-6', user: 'Alex Morgan', action: 'Deactivated user account', module: 'Users', date: addDays(-7), time: '13:10', status: 'Success' }
  ];

  localStorage.setItem(DB_KEYS.users, JSON.stringify(users));
  localStorage.setItem(DB_KEYS.doctors, JSON.stringify(doctors));
  localStorage.setItem(DB_KEYS.patients, JSON.stringify(patients));
  localStorage.setItem(DB_KEYS.appointments, JSON.stringify(appointments));
  localStorage.setItem(DB_KEYS.consultations, JSON.stringify(consultations));
  localStorage.setItem(DB_KEYS.documents, JSON.stringify(documents));
  localStorage.setItem(DB_KEYS.notifications, JSON.stringify(notifications));
  localStorage.setItem(DB_KEYS.auditLogs, JSON.stringify(auditLogs));
  localStorage.setItem(DB_KEYS.seeded, 'true');
}

/* ---------- date helpers ---------- */
function today() {
  return new Date().toISOString().slice(0, 10);
}
function addDays(n) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
}
function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}
function formatDateLong(iso) {
  if (!iso) return '—';
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
}

/* ---------- generic DB accessors ---------- */
const DB = {
  get(key) { return JSON.parse(localStorage.getItem(key) || '[]'); },
  set(key, value) { localStorage.setItem(key, JSON.stringify(value)); },
  find(key, id) { return this.get(key).find(x => x.id === id); },
  update(key, id, patch) {
    const items = this.get(key);
    const idx = items.findIndex(x => x.id === id);
    if (idx > -1) { items[idx] = { ...items[idx], ...patch }; this.set(key, items); }
    return idx > -1 ? items[idx] : null;
  },
  insert(key, item) {
    const items = this.get(key);
    items.unshift(item);
    this.set(key, items);
    return item;
  },
  remove(key, id) {
    const items = this.get(key).filter(x => x.id !== id);
    this.set(key, items);
  }
};

function addAuditLog(user, action, module, status = 'Success') {
  const now = new Date();
  DB.insert(DB_KEYS.auditLogs, {
    id: 'AL-' + Date.now(),
    user, action, module,
    date: now.toISOString().slice(0, 10),
    time: now.toTimeString().slice(0, 5),
    status
  });
}

function addNotification(userId, title, message, type = 'system') {
  DB.insert(DB_KEYS.notifications, {
    id: 'N-' + Date.now(),
    userId, title, message, type,
    date: today(),
    read: false
  });
}

seedDatabase();
