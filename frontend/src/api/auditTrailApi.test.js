import { describe, it, expect, vi, beforeEach } from 'vitest';
import { auditTrailApi } from './auditTrailApi';

vi.mock('./axiosInstance', () => ({
  default: {
    get: vi.fn(),
  },
}));

import axiosInstance from './axiosInstance';

describe('auditTrailApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getAll should GET /audit-trail with params', async () => {
    axiosInstance.get.mockResolvedValue({ data: {} });
    const params = { page: 0, size: 10 };
    await auditTrailApi.getAll(params);
    expect(axiosInstance.get).toHaveBeenCalledWith('/audit-trail', { params });
  });

  it('getByEntity should GET /audit-trail/entity/:type/:id', async () => {
    axiosInstance.get.mockResolvedValue({ data: {} });
    await auditTrailApi.getByEntity('USER', 5);
    expect(axiosInstance.get).toHaveBeenCalledWith('/audit-trail/entity/USER/5');
  });
});
