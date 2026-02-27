import { render } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';

export function renderWithRouter(ui, options = {}) {
  function Wrapper({ children }) {
    return <BrowserRouter>{children}</BrowserRouter>;
  }
  return render(ui, { wrapper: Wrapper, ...options });
}

export function renderWithProviders(ui, options = {}) {
  function Wrapper({ children }) {
    return (
      <BrowserRouter>
        <AuthProvider>{children}</AuthProvider>
      </BrowserRouter>
    );
  }
  return render(ui, { wrapper: Wrapper, ...options });
}

export function createMockAuthContext(overrides = {}) {
  const defaultUser = {
    id: 1,
    username: 'admin',
    fullName: 'Admin User',
    roles: ['ADMIN'],
    email: 'admin@test.com',
  };
  const user = overrides.user !== undefined ? overrides.user : defaultUser;
  return {
    user,
    loading: false,
    login: vi.fn(),
    logout: vi.fn(),
    hasRole: vi.fn((role) => user?.roles?.includes(role) ?? false),
    hasAnyRole: vi.fn((...roles) => roles.some((r) => user?.roles?.includes(r) ?? false)),
    fetchUser: vi.fn(),
    ...overrides,
  };
}

export * from '@testing-library/react';
