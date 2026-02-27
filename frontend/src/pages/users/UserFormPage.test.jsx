import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import UserFormPage from './UserFormPage';

vi.mock('../../api/userApi', () => ({
  userApi: {
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
  },
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { error: vi.fn(), success: vi.fn() } };
});

import { userApi } from '../../api/userApi';

describe('UserFormPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render create form with all fields including username', () => {
    render(
      <MemoryRouter initialEntries={['/users/new']}>
        <Routes>
          <Route path="/users/new" element={<UserFormPage />} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText('Create New User')).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByText('Submit Create')).toBeInTheDocument();
  });

  it('should render edit form and load user data', async () => {
    userApi.getById.mockResolvedValue({
      data: {
        data: {
          id: 1,
          username: 'admin',
          email: 'admin@test.com',
          fullName: 'Admin User',
          phone: '123',
          roles: ['ADMIN'],
          isActive: true,
          version: 0,
        },
      },
    });

    render(
      <MemoryRouter initialEntries={['/users/1/edit']}>
        <Routes>
          <Route path="/users/:id/edit" element={<UserFormPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Edit User')).toBeInTheDocument();
    });
    expect(screen.getByText('Submit Update')).toBeInTheDocument();
    // Username should not be shown in edit mode
    expect(screen.queryByLabelText('Username')).not.toBeInTheDocument();
  });

  it('should show back button', () => {
    render(
      <MemoryRouter initialEntries={['/users/new']}>
        <Routes>
          <Route path="/users/new" element={<UserFormPage />} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText('Back')).toBeInTheDocument();
  });
});
