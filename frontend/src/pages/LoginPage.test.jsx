import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
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

import { useAuth } from '../auth/AuthContext';

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render login page with Keycloak sign-in button', () => {
    useAuth.mockReturnValue({ user: null, loading: false, login: vi.fn() });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    expect(screen.getByText('User Management')).toBeInTheDocument();
    expect(screen.getByText('Sign in to continue to the District Management System')).toBeInTheDocument();
    expect(screen.getByText('Sign In with Keycloak')).toBeInTheDocument();
  });

  it('should redirect to dashboard if user already logged in', () => {
    useAuth.mockReturnValue({ user: { id: 1 }, loading: false, login: vi.fn() });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
  });

  it('should call login when Sign In with Keycloak is clicked', () => {
    const loginMock = vi.fn();
    useAuth.mockReturnValue({ user: null, loading: false, login: loginMock });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    fireEvent.click(screen.getByText('Sign In with Keycloak'));
    expect(loginMock).toHaveBeenCalled();
  });
});
