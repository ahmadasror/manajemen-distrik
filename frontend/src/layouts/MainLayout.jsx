import { useState } from 'react';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  ClipboardCheck,
  ScrollText,
  ShieldCheck,
  LogOut,
  Menu,
  X,
  ChevronRight,
  User,
  Map,
  Search,
  Upload,
  Settings,
} from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { usePermission } from '../hooks/usePermission';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { cn } from '@/lib/utils';

const navItems = [
  { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { path: '/users', icon: Users, label: 'Users' },
  { path: '/pending-actions', icon: ClipboardCheck, label: 'Pending Actions' },
  { path: '/audit-trail', icon: ScrollText, label: 'Audit Trail' },
];

function NavItem({ item, isActive, collapsed, onClick }) {
  const Icon = item.icon;
  return (
    <button
      onClick={onClick}
      className={cn(
        'flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150',
        isActive
          ? 'bg-white/15 text-white'
          : 'text-slate-300 hover:bg-white/10 hover:text-white',
        collapsed && 'justify-center px-2'
      )}
      title={collapsed ? item.label : undefined}
    >
      <Icon className="h-5 w-5 shrink-0" />
      {!collapsed && <span>{item.label}</span>}
      {!collapsed && isActive && <ChevronRight className="ml-auto h-4 w-4 opacity-60" />}
    </button>
  );
}

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const { isAdmin, canCreate } = usePermission();

  const allNavItems = [
    ...navItems,
    ...(isAdmin ? [{ path: '/roles', icon: ShieldCheck, label: 'Roles' }] : []),
    { path: '/wilayah', icon: Map, label: 'Data Wilayah' },
    { path: '/wilayah/inquiry', icon: Search, label: 'Inquiry Wilayah' },
    ...((isAdmin || canCreate) ? [{ path: '/wilayah/bulk-upload', icon: Upload, label: 'Bulk Upload' }] : []),
    ...(isAdmin ? [{ path: '/settings', icon: Settings, label: 'Pengaturan' }] : []),
  ];

  const activeBase = location.pathname;

  const initials = user?.fullName
    ? user.fullName.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : 'U';

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      {/* Sidebar */}
      <aside
        className={cn(
          'flex flex-col bg-slate-900 text-white transition-all duration-300 shrink-0',
          collapsed ? 'w-16' : 'w-60'
        )}
      >
        {/* Logo */}
        <div className={cn(
          'flex items-center h-16 px-4 border-b border-slate-700',
          collapsed ? 'justify-center' : 'gap-2'
        )}>
          <div className="flex items-center justify-center h-8 w-8 rounded-lg bg-primary shrink-0">
            <Map className="h-5 w-5 text-white" />
          </div>
          {!collapsed && (
            <span className="font-semibold text-base tracking-tight">Manajemen Distrik</span>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1">
          {allNavItems.map((item) => (
            <NavItem
              key={item.path}
              item={item}
              isActive={activeBase === item.path || (item.path !== '/' && activeBase.startsWith(item.path + '/') && !allNavItems.some((other) => other !== item && activeBase.startsWith(other.path) && other.path.length > item.path.length))}
              collapsed={collapsed}
              onClick={() => navigate(item.path)}
            />
          ))}
        </nav>

        {/* Collapse toggle */}
        <div className="p-3 border-t border-slate-700">
          <button
            onClick={() => setCollapsed(!collapsed)}
            className={cn(
              'flex items-center gap-2 w-full px-3 py-2 rounded-lg text-slate-400 hover:bg-white/10 hover:text-white text-sm transition-colors',
              collapsed && 'justify-center'
            )}
          >
            {collapsed ? <Menu className="h-5 w-5" /> : <X className="h-4 w-4" />}
            {!collapsed && <span>Collapse</span>}
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-col flex-1 min-w-0">
        {/* Header */}
        <header className="flex items-center justify-between h-16 px-6 border-b bg-background shrink-0">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold text-foreground">
              {allNavItems.slice().sort((a, b) => b.path.length - a.path.length).find((i) => activeBase === i.path || activeBase.startsWith(i.path + '/'))?.label || 'Dashboard'}
            </h1>
          </div>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="flex items-center gap-2 h-9 px-2">
                <Avatar className="h-7 w-7">
                  <AvatarFallback className="bg-primary text-primary-foreground text-xs">
                    {initials}
                  </AvatarFallback>
                </Avatar>
                <span className="text-sm font-medium hidden sm:block">{user?.fullName}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div>
                  <p className="font-medium">{user?.fullName}</p>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    {user?.roles?.join(', ')}
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                className="text-destructive focus:text-destructive cursor-pointer"
                onClick={logout}
              >
                <LogOut className="h-4 w-4 mr-2" />
                Logout
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6 bg-muted/30">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
