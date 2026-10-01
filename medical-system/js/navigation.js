/* ==========================================================================
   navigation.js — Builds sidebar & topbar for authenticated portals
   ========================================================================== */

const NAV_ITEMS = {
  patient: [
    { label: 'Dashboard', icon: 'fa-gauge-high', href: 'dashboard.html' },
    { label: 'Book Appointment', icon: 'fa-calendar-plus', href: 'book-appointment.html' },
    { label: 'My Appointments', icon: 'fa-calendar-check', href: 'appointments.html' },
    { label: 'Medical History', icon: 'fa-file-waveform', href: 'medical-history.html' },
    { label: 'Documents', icon: 'fa-folder-open', href: 'documents.html' },
    { label: 'Notifications', icon: 'fa-bell', href: 'notifications.html' },
    { label: 'Profile', icon: 'fa-user', href: 'profile.html' },
    { label: 'Settings', icon: 'fa-gear', href: 'settings.html' }
  ],
  doctor: [
    { label: 'Dashboard', icon: 'fa-gauge-high', href: 'dashboard.html' },
    { label: 'Agenda', icon: 'fa-calendar-days', href: 'agenda.html' },
    { label: 'Patients', icon: 'fa-users', href: 'patients.html' },
    { label: 'Register Consultation', icon: 'fa-notes-medical', href: 'consultation.html' },
    { label: 'Medical History', icon: 'fa-file-waveform', href: 'medical-history.html' },
    { label: 'Schedule', icon: 'fa-clock', href: 'schedule.html' },
    { label: 'Notifications', icon: 'fa-bell', href: 'notifications.html' },
    { label: 'Profile', icon: 'fa-user', href: 'profile.html' }
  ],
  receptionist: [
    { label: 'Dashboard', icon: 'fa-gauge-high', href: 'dashboard.html' },
    { label: 'Appointments', icon: 'fa-calendar-check', href: 'appointments.html' },
    { label: 'Patients', icon: 'fa-users', href: 'patients.html' },
    { label: 'Check-in', icon: 'fa-clipboard-check', href: 'check-in.html' },
    { label: 'Notifications', icon: 'fa-bell', href: 'notifications.html' },
    { label: 'Profile', icon: 'fa-user', href: 'profile.html' }
  ],
  admin: [
    { label: 'Dashboard', icon: 'fa-gauge-high', href: 'dashboard.html' },
    { label: 'Users', icon: 'fa-user-gear', href: 'users.html' },
    { label: 'Doctors', icon: 'fa-user-doctor', href: 'doctors.html' },
    { label: 'Patients', icon: 'fa-users', href: 'patients.html' },
    { label: 'Specialties', icon: 'fa-stethoscope', href: 'specialties.html' },
    { label: 'Locations', icon: 'fa-location-dot', href: 'locations.html' },
    { label: 'Schedules', icon: 'fa-clock', href: 'schedules.html' },
    { label: 'Reports', icon: 'fa-chart-line', href: 'reports.html' },
    { label: 'Audit Logs', icon: 'fa-list-check', href: 'audit-logs.html' }
  ]
};

const ROLE_LABELS = { patient: 'Patient', doctor: 'Doctor', receptionist: 'Receptionist', admin: 'Administrator' };

function renderPortalLayout(activeHref) {
  const user = Auth.currentUser();
  if (!user) return;
  const items = NAV_ITEMS[user.role] || [];
  const unreadCount = 0; // se completa de forma asíncrona (solo pacientes)

  const sidebarHtml = `
    <aside class="sidebar" id="sidebar">
      <div class="sidebar-brand">
        <i class="fa-solid fa-heart-pulse"></i>
        <span>MediCare<em>+</em></span>
        <button class="sidebar-close" id="sidebarClose" aria-label="Close menu"><i class="fa-solid fa-xmark"></i></button>
      </div>
      <nav class="sidebar-nav">
        ${items.map(it => `
          <a href="${it.href}" class="sidebar-link ${activeHref === it.href ? 'active' : ''}">
            <i class="fa-solid ${it.icon}"></i><span>${it.label}</span>
          </a>`).join('')}
      </nav>
      <div class="sidebar-footer">
        <a href="#" id="logoutBtn" class="sidebar-link logout-link"><i class="fa-solid fa-right-from-bracket"></i><span>Logout</span></a>
      </div>
    </aside>
    <div class="sidebar-overlay" id="sidebarOverlay"></div>`;

  const topbarHtml = `
    <header class="topbar">
      <button class="hamburger" id="hamburgerBtn" aria-label="Open menu"><i class="fa-solid fa-bars"></i></button>
      <div class="topbar-title">${ROLE_LABELS[user.role]} Portal</div>
      <div class="topbar-actions">
        <a href="notifications.html" class="icon-btn" title="Notifications">
          <i class="fa-solid fa-bell"></i>
          ${unreadCount > 0 ? `<span class="badge-dot">${unreadCount}</span>` : ''}
        </a>
        <div class="profile-menu">
          <button class="profile-trigger" id="profileTrigger">
            <span class="avatar-sm">${initials(user.name)}</span>
            <span class="profile-name">${user.name}</span>
            <i class="fa-solid fa-chevron-down"></i>
          </button>
          <div class="profile-dropdown" id="profileDropdown">
            <a href="profile.html"><i class="fa-solid fa-user"></i> My Profile</a>
            <a href="#" id="logoutBtn2"><i class="fa-solid fa-right-from-bracket"></i> Logout</a>
          </div>
        </div>
      </div>
    </header>`;

  const sidebarMount = document.getElementById('sidebar-mount');
  const topbarMount = document.getElementById('topbar-mount');
  if (sidebarMount) sidebarMount.innerHTML = sidebarHtml;
  if (topbarMount) topbarMount.innerHTML = topbarHtml;

  // Notificaciones sin leer (el backend las expone solo para el paciente)
  if (user.role === 'patient' && typeof Api !== 'undefined') {
    Api.notificaciones.mias().then(list => {
      const n = list.filter(x => !x.leido).length;
      const bell = document.querySelector('.topbar-actions .icon-btn');
      if (bell && n > 0 && !bell.querySelector('.badge-dot')) bell.insertAdjacentHTML('beforeend', `<span class="badge-dot">${n}</span>`);
    }).catch(() => { /* sin conexión: no se muestra el contador */ });
  }

  // Interactions
  const doLogout = (e) => { e.preventDefault(); Auth.logout(); };
  document.getElementById('logoutBtn')?.addEventListener('click', doLogout);
  document.getElementById('logoutBtn2')?.addEventListener('click', doLogout);

  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('sidebarOverlay');
  document.getElementById('hamburgerBtn')?.addEventListener('click', () => { sidebar.classList.add('open'); overlay.classList.add('show'); });
  document.getElementById('sidebarClose')?.addEventListener('click', () => { sidebar.classList.remove('open'); overlay.classList.remove('show'); });
  overlay?.addEventListener('click', () => { sidebar.classList.remove('open'); overlay.classList.remove('show'); });

  const profileTrigger = document.getElementById('profileTrigger');
  const profileDropdown = document.getElementById('profileDropdown');
  profileTrigger?.addEventListener('click', () => profileDropdown.classList.toggle('show'));
  document.addEventListener('click', (e) => {
    if (profileTrigger && !profileTrigger.contains(e.target) && !profileDropdown.contains(e.target)) {
      profileDropdown?.classList.remove('show');
    }
  });
}

function renderPublicNavbar(activePage) {
  const mount = document.getElementById('public-navbar-mount');
  if (!mount) return;
  const user = Auth.currentUser();
  const base = getBasePath();
  const links = [
    { label: 'Home', href: base + 'index.html', key: 'home' },
    { label: 'Specialties', href: base + 'pages/public/specialties.html', key: 'specialties' },
    { label: 'Doctors', href: base + 'pages/public/doctors.html', key: 'doctors' },
    { label: 'About Us', href: base + 'pages/public/about.html', key: 'about' },
    { label: 'Contact', href: base + 'pages/public/contact.html', key: 'contact' }
  ];
  mount.innerHTML = `
    <nav class="navbar">
      <div class="navbar-inner">
        <a href="${base}index.html" class="navbar-brand"><i class="fa-solid fa-heart-pulse"></i> MediCare<em>+</em></a>
        <button class="navbar-toggle" id="navToggle" aria-label="Toggle menu"><i class="fa-solid fa-bars"></i></button>
        <div class="navbar-links" id="navLinks">
          ${links.map(l => `<a href="${l.href}" class="${activePage === l.key ? 'active' : ''}">${l.label}</a>`).join('')}
          ${user ? `
            <a href="${dashboardFor(user.role)}" class="btn btn-outline btn-sm">Dashboard</a>
          ` : `
            <a href="${base}pages/auth/login.html" class="btn btn-outline btn-sm">Login</a>
            <a href="${base}pages/auth/register.html" class="btn btn-primary btn-sm">Register</a>
          `}
        </div>
      </div>
    </nav>`;
  document.getElementById('navToggle')?.addEventListener('click', () => {
    document.getElementById('navLinks').classList.toggle('show');
  });
}

function renderFooter() {
  const mount = document.getElementById('footer-mount');
  if (!mount) return;
  const base = getBasePath();
  mount.innerHTML = `
    <footer class="site-footer">
      <div class="footer-grid">
        <div>
          <div class="navbar-brand" style="color:#fff;"><i class="fa-solid fa-heart-pulse"></i> MediCare<em>+</em></div>
          <p>Modern healthcare management for patients, doctors and clinics — book, manage and track care in one place.</p>
        </div>
        <div>
          <h5>Platform</h5>
          <a href="${base}pages/public/specialties.html">Specialties</a>
          <a href="${base}pages/public/doctors.html">Find a Doctor</a>
          <a href="${base}pages/auth/register.html">Create Account</a>
        </div>
        <div>
          <h5>Company</h5>
          <a href="${base}pages/public/about.html">About Us</a>
          <a href="${base}pages/public/contact.html">Contact</a>
          <a href="${base}pages/help/faq.html">FAQ</a>
        </div>
        <div>
          <h5>Legal</h5>
          <a href="${base}pages/help/privacy.html">Privacy Policy</a>
          <a href="${base}pages/help/terms.html">Terms &amp; Conditions</a>
          <a href="${base}pages/help/help-center.html">Help Center</a>
        </div>
      </div>
      <div class="footer-bottom">© ${new Date().getFullYear()} MediCare+. All rights reserved. This is a demo frontend with simulated data.</div>
    </footer>`;
}
