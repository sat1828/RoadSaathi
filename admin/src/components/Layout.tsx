import { useState, useEffect } from 'react';
import { Outlet, NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: '◈' },
  { to: '/map', label: 'Live Map', icon: '⌂' },
  { to: '/triage', label: 'Triage', icon: '⊞' },
  { to: '/status-board', label: 'Status Board', icon: '☰' },
  { to: '/analytics', label: 'Analytics', icon: '◉' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [dark, setDark] = useState(() => localStorage.getItem('theme') !== 'light');
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  }, [dark]);

  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          style={{
            position: 'fixed', inset: 0, background: 'var(--overlay)',
            zIndex: 40, backdropFilter: 'blur(4px)',
          }}
        />
      )}

      {/* Sidebar */}
      <aside style={{
        position: 'fixed', top: 0, left: 0, bottom: 0, zIndex: 50,
        width: 260, background: 'var(--sidebar-bg)',
        backdropFilter: 'blur(20px) saturate(180%)',
        borderRight: '1px solid rgba(255,255,255,0.06)',
        display: 'flex', flexDirection: 'column',
        transform: sidebarOpen ? 'translateX(0)' : 'translateX(-100%)',
        transition: 'transform 0.3s cubic-bezier(0.4,0,0.2,1)',
      }}>
        <div style={{ padding: '1.5rem 1.25rem', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, background: 'var(--gradient-primary)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            RoadSaathi
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2 }}>Admin Console</div>
        </div>

        <nav style={{ flex: 1, padding: '1rem 0.75rem', display: 'flex', flexDirection: 'column', gap: 2 }}>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              style={({ isActive }) => ({
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '0.75rem 1rem', borderRadius: 12,
                textDecoration: 'none', fontSize: '0.9rem', fontWeight: 500,
                color: isActive ? '#fff' : 'var(--sidebar-text)',
                background: isActive ? 'var(--sidebar-active)' : 'transparent',
                transition: 'all 0.2s ease',
              })}
            >
              <span style={{ fontSize: '1.1rem' }}>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* User section */}
        <div style={{ padding: '1rem 0.75rem', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '0.5rem 0.75rem', borderRadius: 12, background: 'rgba(255,255,255,0.04)' }}>
            <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'var(--gradient-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 600, fontSize: '0.8rem' }}>
              {user?.name?.charAt(0)?.toUpperCase() || 'U'}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ color: 'var(--sidebar-text)', fontSize: '0.85rem', fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {user?.name || 'User'}
              </div>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>{user?.role || ''}</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Mobile sidebar toggle — visible on small screens */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        style={{
          position: 'fixed', top: 12, left: 12, zIndex: 60,
          width: 40, height: 40, borderRadius: 12,
          background: 'var(--bg-glass)', backdropFilter: 'blur(12px)',
          border: '1px solid var(--border-glass)',
          color: 'var(--text-primary)', fontSize: '1.2rem',
          cursor: 'pointer', display: 'flex', alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {sidebarOpen ? '✕' : '☰'}
      </button>

      {/* Main content */}
      <main style={{
        flex: 1, marginLeft: 0,
        minHeight: '100vh', display: 'flex', flexDirection: 'column',
      }}>
        {/* Top bar */}
        <header style={{
          display: 'flex', alignItems: 'center', justifyContent: 'flex-end',
          padding: '0.75rem 1.5rem', gap: 12,
          background: 'var(--bg-glass)', backdropFilter: 'blur(16px) saturate(180%)',
          borderBottom: '1px solid var(--border-glass)',
          position: 'sticky', top: 0, zIndex: 30,
        }}>
          <button
            onClick={() => setDark(!dark)}
            style={{
              padding: '0.5rem 1rem', borderRadius: 10,
              border: '1px solid var(--border-glass)',
              background: 'var(--bg-glass)', cursor: 'pointer',
              color: 'var(--text-primary)', fontSize: '0.85rem',
              display: 'flex', alignItems: 'center', gap: 6,
              transition: 'all 0.2s',
            }}
            title={`Switch to ${dark ? 'light' : 'dark'} mode`}
          >
            {dark ? '☀️ Light' : '🌙 Dark'}
          </button>
          <button
            onClick={logout}
            style={{
              padding: '0.5rem 1rem', borderRadius: 10,
              border: '1px solid var(--accent-danger)',
              background: 'transparent', cursor: 'pointer',
              color: 'var(--accent-danger)', fontSize: '0.85rem',
              fontWeight: 500,
            }}
          >
            Logout
          </button>
        </header>

        {/* Page content */}
        <div style={{ flex: 1, padding: '1.5rem' }}>
          <Outlet />
        </div>
      </main>

      <style>{`
        @media (min-width: 768px) {
          aside { transform: translateX(0) !important; }
          main { margin-left: 260px; }
          header > button:first-of-type { display: flex !important; }
          header > button:nth-of-type(2) { display: none; }
        }
      `}</style>
    </div>
  );
}
