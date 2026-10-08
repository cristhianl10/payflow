import { useState } from 'react';
import {
  Link,
  NavLink,
  Navigate,
  Outlet,
  useLocation,
  useNavigate,
} from 'react-router-dom';
import {
  ArrowUpRight,
  LayoutDashboard,
  List,
  Wallet,
  UserRound,
  UsersRound,
  LogOut,
  ShieldCheck,
  Bell,
  CalendarClock,
  FileText,
  Menu,
  X,
} from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useSession } from '../hooks/useSession';
import { api, logout } from '../services/api';
import { Avatar, Loading, Logo, Notice } from '../components/ui';

const navigation = [
  { to: '/app', end: true, label: 'Overview', Icon: LayoutDashboard },
  { to: '/app/send', label: 'Send money', Icon: ArrowUpRight },
  { to: '/app/scheduled-transfers', label: 'Scheduled', Icon: CalendarClock },
  { to: '/app/transactions', label: 'Activity', Icon: List },
  { to: '/app/reports', label: 'Reports', Icon: FileText },
  { to: '/app/notifications', label: 'Notifications', Icon: Bell },
  { to: '/app/beneficiaries', label: 'Beneficiaries', Icon: UsersRound },
  { to: '/app/wallet', label: 'Your wallet', Icon: Wallet },
  { to: '/app/account', label: 'Account', Icon: UserRound },
];

export function AppLayout() {
  const session = useSession();
  const [menu, setMenu] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  const queries = useQueryClient();
  const unread = useQuery({
    queryKey: ['notifications-unread'],
    queryFn: () => api<{ count: number }>('/notifications/unread-count'),
    enabled: !!session,
    refetchInterval: 30_000,
  });
  const navigate = useNavigate();
  const location = useLocation();
  if (session === undefined) return <Loading />;
  if (!session)
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  const name = `${session.user.firstName} ${session.user.lastName}`;
  const current = navigation.find((item) =>
    item.end
      ? location.pathname === item.to
      : location.pathname.startsWith(item.to),
  );
  return (
    <div className="workspace">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <aside className={`sidebar ${menu ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <Logo to="/app" />
          <button
            className="icon-button mobile-menu"
            aria-label={menu ? 'Close navigation' : 'Open navigation'}
            aria-expanded={menu}
            aria-controls="workspace-navigation"
            onClick={() => setMenu(!menu)}
          >
            {menu ? <X /> : <Menu />}
          </button>
        </div>
        <nav
          id="workspace-navigation"
          aria-label="Main navigation"
          className="main-navigation"
        >
          {navigation.map(({ to, end, label, Icon }) => (
            <NavLink
              key={to}
              end={end}
              to={to}
              onClick={() => setMenu(false)}
              className={({ isActive }) =>
                `nav-item ${isActive ? 'nav-item-active' : ''}`
              }
            >
              <Icon size={19} aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sandbox-note">
            <ShieldCheck size={21} aria-hidden="true" />
            <strong>A space to explore</strong>
            <p>Every dollar here is simulated. No bank account needed.</p>
          </div>
          <button
            className="nav-item sign-out"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              setError(undefined);
              try {
                await logout();
                queries.clear();
                navigate('/login');
              } catch (failure) {
                setError(failure);
              } finally {
                setBusy(false);
              }
            }}
          >
            <LogOut size={18} aria-hidden="true" />
            {busy ? 'Signing out…' : 'Sign out'}
          </button>
          <Notice error={error} />
        </div>
      </aside>
      <div className="workspace-main">
        <header className="topbar">
          <span className="breadcrumb">
            Workspace <span aria-hidden="true">/</span>{' '}
            <strong>{current?.label ?? 'Transaction details'}</strong>
          </span>
          <div className="topbar-user">
            <Link
              className="icon-button"
              to="/app/notifications"
              aria-label={
                unread.data?.count
                  ? `Notifications, ${unread.data.count} unread`
                  : 'Notifications'
              }
            >
              <Bell size={18} aria-hidden="true" />
              {!!unread.data?.count && (
                <span className="sandbox-badge">{unread.data.count}</span>
              )}
            </Link>
            <span className="sandbox-badge">Sandbox</span>
            <Avatar name={name} small />
            <span className="topbar-name">{session.user.firstName}</span>
          </div>
        </header>
        <main
          id="main-content"
          className="page-content"
          key={location.pathname}
        >
          <Outlet />
        </main>
        <footer className="workspace-footer">
          <span>PayFlow · Educational sandbox</span>
          <span>No real money is processed by PayFlow.</span>
        </footer>
      </div>
    </div>
  );
}
