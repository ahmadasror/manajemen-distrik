import { describe, it, expect, vi, beforeEach } from 'vitest';
import { authApi } from './authApi';

vi.mock('./axiosInstance', () => ({
  default: {
    get: vi.fn(),
  },
}));

import axiosInstance from './axiosInstance';

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('me should GET /auth/me', async () => {
    axiosInstance.get.mockResolvedValue({ data: { success: true } });
    await authApi.me();
    expect(axiosInstance.get).toHaveBeenCalledWith('/auth/me');
  });
});
