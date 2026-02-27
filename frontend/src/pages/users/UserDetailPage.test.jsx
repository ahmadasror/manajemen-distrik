import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import UserDetailPage from './UserDetailPage';

vi.mock('../../api/userApi', () => ({ userApi: { getById: vi.fn() } }));
vi.mock('../../api/auditTrailApi', () => ({ auditTrailApi: { getByEntity: vi.fn() } }));
vi.mock('../../hooks/usePermission', () => ({ usePermission: vi.fn() }));
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { error: vi.fn(), success: vi.fn() } };
});

import { userApi } from '../../api/userApi';
import { auditTrailApi } from '../../api/auditTrailApi';
import { usePermission } from '../../hooks/usePermission';

describe('UserDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    usePermission.mockReturnValue({ canCreate: true });
  });

  it('should render user details after loading', async () => {
    userApi.getById.mockResolvedValue({
      data: {
        data: {
          id: 1, username: 'admin', fullName: 'Admin User', email: 'admin@test.com',
          phone: '123', isActive: true, roles: ['ADMIN'], version: 0,
          createdAt: '2024-01-01', updatedAt: '2024-01-01', createdBy: 'system', updatedBy: 'system',
        },
      },
    });
    auditTrailApi.getByEntity.mockResolvedValue({ data: { data: { content: [] } } });

    render(
      <MemoryRouter initialEntries={['/users/1']}>
        <Routes><Route path="/users/:id" element={<UserDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    expect(screen.getByText('Admin User')).toBeInTheDocument();
    expect(screen.getByText('admin@test.com')).toBeInTheDocument();
  });

  it('should show Edit button when canCreate is true', async () => {
    usePermission.mockReturnValue({ canCreate: true });
    userApi.getById.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', fullName: 'Admin', email: 'a@t.com', isActive: true, roles: ['ADMIN'], version: 0, createdAt: '2024-01-01', updatedAt: '2024-01-01' } },
    });
    auditTrailApi.getByEntity.mockResolvedValue({ data: { data: { content: [] } } });

    render(
      <MemoryRouter initialEntries={['/users/1']}>
        <Routes><Route path="/users/:id" element={<UserDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('Edit')).toBeInTheDocument());
  });

  it('should hide Edit button when canCreate is false', async () => {
    usePermission.mockReturnValue({ canCreate: false });
    userApi.getById.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', fullName: 'Admin', email: 'a@t.com', isActive: true, roles: ['ADMIN'], version: 0, createdAt: '2024-01-01', updatedAt: '2024-01-01' } },
    });
    auditTrailApi.getByEntity.mockResolvedValue({ data: { data: { content: [] } } });

    render(
      <MemoryRouter initialEntries={['/users/1']}>
        <Routes><Route path="/users/:id" element={<UserDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument());
    expect(screen.queryByText('Edit')).not.toBeInTheDocument();
  });

  it('should show User not found when user is null', async () => {
    userApi.getById.mockRejectedValue(new Error('Not found'));
    auditTrailApi.getByEntity.mockRejectedValue(new Error('Not found'));

    render(
      <MemoryRouter initialEntries={['/users/1']}>
        <Routes><Route path="/users/:id" element={<UserDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('User not found')).toBeInTheDocument());
  });
});
