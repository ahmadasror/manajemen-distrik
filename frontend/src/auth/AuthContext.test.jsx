import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('./keycloak', () => ({
  default: {
    authenticated: false,
    init: vi.fn().mockResolvedValue(undefined),
    login: vi.fn(),
    logout: vi.fn(),
    updateToken: vi.fn(),
  },
}));

vi.mock('../api/authApi', () => ({
  authApi: {
    me: vi.fn(),
  },
}));

import keycloak from './keycloak';
import { authApi } from '../api/authApi';

function wrapper({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    keycloak.authenticated = false;
    keycloak.init.mockResolvedValue(undefined);
  });

  it('useAuth outside provider should throw', () => {
    expect(() => {
      renderHook(() => useAuth());
    }).toThrow('useAuth must be used within AuthProvider');
  });

  it('should set loading to false and user to null when keycloak is not authenticated', async () => {
    keycloak.authenticated = false;
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.user).toBeNull();
    expect(authApi.me).not.toHaveBeenCalled();
  });

  it('should call me endpoint and set user when keycloak is authenticated', async () => {
    keycloak.authenticated = true;
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['ADMIN'] } },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.user).toEqual({ id: 1, username: 'admin', roles: ['ADMIN'] });
    expect(authApi.me).toHaveBeenCalled();
  });

  it('login should call keycloak.login', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.login();
    });

    expect(keycloak.login).toHaveBeenCalled();
  });

  it('logout should call keycloak.logout', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.logout();
    });

    expect(keycloak.logout).toHaveBeenCalled();
  });

  it('hasRole should return true if user has role', async () => {
    keycloak.authenticated = true;
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['ADMIN', 'MAKER'] } },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.user).toBeTruthy());

    expect(result.current.hasRole('ADMIN')).toBe(true);
    expect(result.current.hasRole('VIEWER')).toBe(false);
  });

  it('hasAnyRole should return true if user has any specified role', async () => {
    keycloak.authenticated = true;
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['CHECKER'] } },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.user).toBeTruthy());

    expect(result.current.hasAnyRole('ADMIN', 'CHECKER')).toBe(true);
    expect(result.current.hasAnyRole('ADMIN', 'MAKER')).toBe(false);
  });
});
