import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authApi } from './authApi';

vi.mock('./axiosInstance', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}));

import axiosInstance from './axiosInstance';

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('login should POST to /auth/login with credentials', async () => {
    axiosInstance.post.mockResolvedValue({ data: { success: true } });
    const creds = { username: 'admin', password: 'pass' };
    await authApi.login(creds);
    expect(axiosInstance.post).toHaveBeenCalledWith('/auth/login', creds);
  });

  it('refresh should POST to /auth/refresh with token', async () => {
    axiosInstance.post.mockResolvedValue({ data: { success: true } });
    await authApi.refresh('my-token');
    expect(axiosInstance.post).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'my-token' });
  });

  it('logout should POST to /auth/logout', async () => {
    axiosInstance.post.mockResolvedValue({ data: { success: true } });
    await authApi.logout();
    expect(axiosInstance.post).toHaveBeenCalledWith('/auth/logout');
  });

  it('me should GET /auth/me', async () => {
    axiosInstance.get.mockResolvedValue({ data: { success: true } });
    await authApi.me();
    expect(axiosInstance.get).toHaveBeenCalledWith('/auth/me');
  });
});
