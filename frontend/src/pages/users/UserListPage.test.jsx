import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import UserListPage from './UserListPage';

vi.mock('../../api/userApi', () => ({
  userApi: {
    getAll: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('../../hooks/usePermission', () => ({
  usePermission: vi.fn(),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { error: vi.fn(), success: vi.fn() } };
});

import { userApi } from '../../api/userApi';
import { usePermission } from '../../hooks/usePermission';

const mockUsers = {
  data: {
    data: {
      content: [
        { id: 1, username: 'admin', fullName: 'Admin User', email: 'admin@test.com', roles: ['ADMIN'], isActive: true, createdAt: '2024-01-01T00:00:00' },
        { id: 2, username: 'maker1', fullName: 'Maker User', email: 'maker@test.com', roles: ['MAKER'], isActive: true, createdAt: '2024-01-02T00:00:00' },
      ],
      totalElements: 2,
    },
  },
};

describe('UserListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    userApi.getAll.mockResolvedValue(mockUsers);
  });

  it('should render table with user data', async () => {
    usePermission.mockReturnValue({ canCreate: true });
    render(<MemoryRouter><UserListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    expect(screen.getByText('Admin User')).toBeInTheDocument();
    expect(screen.getByText('admin@test.com')).toBeInTheDocument();
  });

  it('should show Add User button when canCreate is true', async () => {
    usePermission.mockReturnValue({ canCreate: true });
    render(<MemoryRouter><UserListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Add User')).toBeInTheDocument();
    });
  });

  it('should hide Add User button when canCreate is false', async () => {
    usePermission.mockReturnValue({ canCreate: false });
    render(<MemoryRouter><UserListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Users')).toBeInTheDocument();
    });
    expect(screen.queryByText('Add User')).not.toBeInTheDocument();
  });

  it('should render search input', async () => {
    usePermission.mockReturnValue({ canCreate: false });
    render(<MemoryRouter><UserListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search users...')).toBeInTheDocument();
    });
  });
});
