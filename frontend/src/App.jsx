import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
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

const themeConfig = {
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 6,
  },
};

export default function App() {
  return (
    <ConfigProvider theme={themeConfig}>
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
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ConfigProvider>
  );
}
