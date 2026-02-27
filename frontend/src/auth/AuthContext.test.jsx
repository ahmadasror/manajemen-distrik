import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  },
}));

import { authApi } from '../api/authApi';

function wrapper({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('useAuth outside provider should throw', () => {
    expect(() => {
      renderHook(() => useAuth());
    }).toThrow('useAuth must be used within AuthProvider');
  });

  it('fetchUser with no token should set loading to false and user to null', async () => {
    authApi.me.mockResolvedValue({ data: { data: null } });
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.user).toBeNull();
    expect(authApi.me).not.toHaveBeenCalled();
  });

  it('fetchUser with token should call me endpoint', async () => {
    localStorage.setItem('accessToken', 'test-token');
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

  it('fetchUser with invalid token should clear tokens', async () => {
    localStorage.setItem('accessToken', 'bad-token');
    authApi.me.mockRejectedValue(new Error('Unauthorized'));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.user).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('login should store tokens and set user', async () => {
    authApi.me.mockRejectedValue(new Error('no token'));
    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    authApi.login.mockResolvedValue({
      data: {
        data: {
          accessToken: 'access123',
          refreshToken: 'refresh123',
          user: { id: 1, username: 'admin', roles: ['ADMIN'] },
        },
      },
    });

    await act(async () => {
      await result.current.login('admin', 'password');
    });

    expect(localStorage.getItem('accessToken')).toBe('access123');
    expect(localStorage.getItem('refreshToken')).toBe('refresh123');
    expect(result.current.user).toEqual({ id: 1, username: 'admin', roles: ['ADMIN'] });
  });

  it('logout should clear tokens and user', async () => {
    localStorage.setItem('accessToken', 'test-token');
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['ADMIN'] } },
    });
    authApi.logout.mockResolvedValue({});

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.user).toBeTruthy();
    });

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('logout should clear tokens even if API call fails', async () => {
    localStorage.setItem('accessToken', 'test-token');
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['ADMIN'] } },
    });
    authApi.logout.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.user).toBeTruthy();
    });

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.user).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
  });

  it('hasRole should return true if user has role', async () => {
    localStorage.setItem('accessToken', 'test-token');
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['ADMIN', 'MAKER'] } },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.user).toBeTruthy();
    });

    expect(result.current.hasRole('ADMIN')).toBe(true);
    expect(result.current.hasRole('VIEWER')).toBe(false);
  });

  it('hasAnyRole should return true if user has any specified role', async () => {
    localStorage.setItem('accessToken', 'test-token');
    authApi.me.mockResolvedValue({
      data: { data: { id: 1, username: 'admin', roles: ['CHECKER'] } },
    });

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.user).toBeTruthy();
    });

    expect(result.current.hasAnyRole('ADMIN', 'CHECKER')).toBe(true);
    expect(result.current.hasAnyRole('ADMIN', 'MAKER')).toBe(false);
  });
});
