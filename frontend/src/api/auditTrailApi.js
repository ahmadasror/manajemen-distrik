import axiosInstance from './axiosInstance';

export const auditTrailApi = {
  getAll: (params) => axiosInstance.get('/audit-trail', { params }),
  getByEntity: (entityType, entityId) =>
    axiosInstance.get(`/audit-trail/entity/${entityType}/${entityId}`),
};
