import axiosInstance from './axiosInstance';

export const rolesApi = {
  getAll: () => axiosInstance.get('/roles'),
  getById: (id) => axiosInstance.get(`/roles/${id}`),
  assignUser: (roleId, userId) => axiosInstance.post(`/roles/${roleId}/users/${userId}`),
  removeUser: (roleId, userId) => axiosInstance.delete(`/roles/${roleId}/users/${userId}`),
};
