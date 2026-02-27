import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate, useLocation: () => ({ state: null }) };
});

vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { error: vi.fn(), success: vi.fn() } };
});

import { useAuth } from '../auth/AuthContext';

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render login form', () => {
    useAuth.mockReturnValue({ user: null, login: vi.fn() });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    expect(screen.getByText('User Management')).toBeInTheDocument();
    expect(screen.getByText('Sign in to your account')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
    expect(screen.getByText('Sign In')).toBeInTheDocument();
  });

  it('should redirect to dashboard if user already logged in', () => {
    useAuth.mockReturnValue({ user: { id: 1 }, login: vi.fn() });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
  });

  it('should call login on form submit', async () => {
    const loginMock = vi.fn().mockResolvedValue({});
    useAuth.mockReturnValue({ user: null, login: loginMock });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);

    fireEvent.change(screen.getByPlaceholderText('Username'), { target: { value: 'admin' } });
    fireEvent.change(screen.getByPlaceholderText('Password'), { target: { value: 'pass123' } });
    fireEvent.click(screen.getByText('Sign In'));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith('admin', 'pass123');
    });
  });
});
