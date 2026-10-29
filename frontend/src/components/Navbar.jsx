import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { UserRole } from '../utils/constants';

export function Navbar() {
  const { user, logout, switchRole, hasRole } = useAuth();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navLinks = [
    {
      name: 'Donor Portal',
      path: '/donor',
      role: UserRole.DONOR,
      active: location.pathname.startsWith('/donor'),
    },
    {
      name: 'Receiver Portal',
      path: '/receiver',
      role: UserRole.RECEIVER,
      active: location.pathname.startsWith('/receiver'),
    },
    {
      name: 'Driver Portal',
      path: '/driver',
      role: UserRole.DRIVER,
      active: location.pathname.startsWith('/driver'),
    },
    {
      name: 'Admin & Operations',
      path: '/admin',
      role: [UserRole.ADMIN, UserRole.COORDINATOR],
      active: location.pathname.startsWith('/admin'),
    },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/90 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8 h-16">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-indigo-500 text-white shadow-md shadow-indigo-200">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2.5"
                  d="M13 10V3L4 14h7v7l9-11h-7z"
                />
              </svg>
            </div>
            <div>
              <span className="font-extrabold text-slate-900 tracking-tight text-lg">
                Disaster<span className="text-indigo-600">Relief</span>Net
              </span>
              <span className="hidden sm:inline-block ml-2 text-[10px] font-bold uppercase tracking-wider bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                Portal
              </span>
            </div>
          </Link>
        </div>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-1">
          {navLinks.map((link) => {
            const isAccessible = hasRole(link.role);
            return (
              <Link
                key={link.name}
                to={link.path}
                className={`px-3 py-2 text-sm font-semibold rounded-lg transition-all ${
                  link.active
                    ? 'bg-indigo-50 text-indigo-700 shadow-sm'
                    : isAccessible
                      ? 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      : 'text-slate-400 hover:text-slate-600'
                }`}
              >
                {link.name}
              </Link>
            );
          })}
        </nav>

        {/* User profile & Role switcher */}
        <div className="hidden sm:flex items-center gap-3">
          {/* Quick Role Switcher for instant navigation & evaluation */}
          <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl text-xs">
            <span className="text-slate-500 font-medium px-1">Role:</span>
            <select
              value={user?.roles?.[0] || UserRole.ADMIN}
              onChange={(e) => switchRole(e.target.value)}
              aria-label="Switch User Role"
              className="bg-white border-0 text-xs font-semibold rounded-lg py-1 px-2 text-slate-700 shadow-sm focus:ring-2 focus:ring-indigo-500 cursor-pointer"
            >
              <option value={UserRole.ADMIN}>Admin / Coord</option>
              <option value={UserRole.DONOR}>Donor</option>
              <option value={UserRole.RECEIVER}>Receiver</option>
              <option value={UserRole.DRIVER}>Driver</option>
            </select>
          </div>

          <div className="flex items-center gap-2 pl-2 border-l border-slate-200">
            <div className="h-8 w-8 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-xs">
              {user?.name ? user.name.charAt(0) : 'U'}
            </div>
            <button
              type="button"
              onClick={logout}
              className="text-xs font-medium text-slate-500 hover:text-rose-600 transition-colors"
            >
              Log out
            </button>
          </div>
        </div>

        {/* Mobile menu button */}
        <div className="flex md:hidden items-center gap-2">
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle Navigation Menu"
            className="rounded-lg p-2 text-slate-600 hover:bg-slate-100"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {mobileMenuOpen ? (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              ) : (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M4 6h16M4 12h16M4 18h16"
                />
              )}
            </svg>
          </button>
        </div>
      </div>

      {/* Mobile menu dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white px-4 pt-3 pb-5 space-y-2">
          {navLinks.map((link) => (
            <Link
              key={link.name}
              to={link.path}
              onClick={() => setMobileMenuOpen(false)}
              className={`block px-3 py-2 rounded-lg text-base font-medium ${
                link.active ? 'bg-indigo-50 text-indigo-700' : 'text-slate-700 hover:bg-slate-50'
              }`}
            >
              {link.name}
            </Link>
          ))}
          <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
            <span className="text-sm font-semibold text-slate-700">{user?.name}</span>
            <button type="button" onClick={logout} className="text-sm font-medium text-rose-600">
              Log out
            </button>
          </div>
        </div>
      )}
    </header>
  );
}
