import axiosInstance from './axiosInstance';

export const pendingActionApi = {
  getAll: (params) => axiosInstance.get('/pending-actions', { params }),
  getById: (id) => axiosInstance.get(`/pending-actions/${id}`),
  approve: (id, remarks) => axiosInstance.post(`/pending-actions/${id}/approve`, { remarks }),
  reject: (id, remarks) => axiosInstance.post(`/pending-actions/${id}/reject`, { remarks }),
  cancel: (id) => axiosInstance.post(`/pending-actions/${id}/cancel`),
};
