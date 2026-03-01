import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from '@/components/ui/sonner';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import MainLayout from './layouts/MainLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import UserListPage from './pages/users/UserListPage';
import UserDetailPage from './pages/users/UserDetailPage';
import UserFormPage from './pages/users/UserFormPage';
import PendingListPage from './pages/pending/PendingListPage';
import PendingDetailPage from './pages/pending/PendingDetailPage';
import AuditListPage from './pages/audit/AuditListPage';
import AuditDetailPage from './pages/audit/AuditDetailPage';
import RoleListPage from './pages/roles/RoleListPage';
import RoleDetailPage from './pages/roles/RoleDetailPage';
import WilayahPage from './pages/wilayah/WilayahPage';
import WilayahInquiryPage from './pages/wilayah/WilayahInquiryPage';
import BulkUploadPage from './pages/wilayah/BulkUploadPage';
import SettingsPage from './pages/settings/SettingsPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="users" element={<UserListPage />} />
            <Route path="users/new" element={<UserFormPage />} />
            <Route path="users/:id" element={<UserDetailPage />} />
            <Route path="users/:id/edit" element={<UserFormPage />} />
            <Route path="pending-actions" element={<PendingListPage />} />
            <Route path="pending-actions/:id" element={<PendingDetailPage />} />
            <Route path="audit-trail" element={<AuditListPage />} />
            <Route path="audit-trail/:id" element={<AuditDetailPage />} />
            <Route path="roles" element={<ProtectedRoute roles={['ADMIN']}><RoleListPage /></ProtectedRoute>} />
            <Route path="roles/:id" element={<ProtectedRoute roles={['ADMIN']}><RoleDetailPage /></ProtectedRoute>} />
            <Route path="wilayah" element={<WilayahPage />} />
            <Route path="wilayah/inquiry" element={<WilayahInquiryPage />} />
            <Route path="wilayah/bulk-upload" element={<ProtectedRoute roles={['ADMIN', 'MAKER']}><BulkUploadPage /></ProtectedRoute>} />
            <Route path="settings" element={<ProtectedRoute roles={['ADMIN']}><SettingsPage /></ProtectedRoute>} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <Toaster />
      </AuthProvider>
    </BrowserRouter>
  );
}
