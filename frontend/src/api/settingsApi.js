import api from './axiosInstance';

export const settingsApi = {
  getValidation: () => api.get('/settings/validation'),
  updateValidation: (data) => api.put('/settings/validation', data),
};
