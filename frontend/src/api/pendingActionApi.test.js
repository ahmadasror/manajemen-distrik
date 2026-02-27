import { describe, it, expect, vi, beforeEach } from 'vitest';
import { pendingActionApi } from './pendingActionApi';

vi.mock('./axiosInstance', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

import axiosInstance from './axiosInstance';

describe('pendingActionApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getAll should GET /pending-actions with params', async () => {
    axiosInstance.get.mockResolvedValue({ data: {} });
    const params = { page: 0, size: 10, status: 'PENDING' };
    await pendingActionApi.getAll(params);
    expect(axiosInstance.get).toHaveBeenCalledWith('/pending-actions', { params });
  });

  it('getById should GET /pending-actions/:id', async () => {
    axiosInstance.get.mockResolvedValue({ data: {} });
    await pendingActionApi.getById(3);
    expect(axiosInstance.get).toHaveBeenCalledWith('/pending-actions/3');
  });

  it('approve should POST /pending-actions/:id/approve with remarks', async () => {
    axiosInstance.post.mockResolvedValue({ data: {} });
    await pendingActionApi.approve(3, 'Looks good');
    expect(axiosInstance.post).toHaveBeenCalledWith('/pending-actions/3/approve', { remarks: 'Looks good' });
  });

  it('reject should POST /pending-actions/:id/reject with remarks', async () => {
    axiosInstance.post.mockResolvedValue({ data: {} });
    await pendingActionApi.reject(3, 'Not valid');
    expect(axiosInstance.post).toHaveBeenCalledWith('/pending-actions/3/reject', { remarks: 'Not valid' });
  });

  it('cancel should POST /pending-actions/:id/cancel', async () => {
    axiosInstance.post.mockResolvedValue({ data: {} });
    await pendingActionApi.cancel(3);
    expect(axiosInstance.post).toHaveBeenCalledWith('/pending-actions/3/cancel');
  });
});
