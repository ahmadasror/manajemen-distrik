import { describe, it, expect, vi, beforeEach } from 'vitest';
import { userApi } from './userApi';

vi.mock('./axiosInstance', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import axiosInstance from './axiosInstance';

describe('userApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getAll should GET /users with params', async () => {
    axiosInstance.get.mockResolvedValue({ data: {} });
    const params = { page: 0, size: 10, search: 'admin' };
    await userApi.getAll(params);
    expect(axiosInstance.get).toHaveBeenCalledWith('/users', { params });
  });

  it('getById should GET /users/:id', async () => {
    axiosInstance.get.mockResolvedValue({ data: {} });
    await userApi.getById(5);
    expect(axiosInstance.get).toHaveBeenCalledWith('/users/5');
  });

  it('create should POST /users with data', async () => {
    axiosInstance.post.mockResolvedValue({ data: {} });
    const data = { username: 'new', email: 'new@test.com' };
    await userApi.create(data);
    expect(axiosInstance.post).toHaveBeenCalledWith('/users', data);
  });

  it('update should PUT /users/:id with data', async () => {
    axiosInstance.put.mockResolvedValue({ data: {} });
    const data = { email: 'updated@test.com' };
    await userApi.update(5, data);
    expect(axiosInstance.put).toHaveBeenCalledWith('/users/5', data);
  });

  it('delete should DELETE /users/:id', async () => {
    axiosInstance.delete.mockResolvedValue({ data: {} });
    await userApi.delete(5);
    expect(axiosInstance.delete).toHaveBeenCalledWith('/users/5');
  });
});
