import axiosInstance from './axiosInstance';

export const authApi = {
  login: (credentials) => axiosInstance.post('/auth/login', credentials),
  refresh: (refreshToken) => axiosInstance.post('/auth/refresh', { refreshToken }),
  logout: () => axiosInstance.post('/auth/logout'),
  me: () => axiosInstance.get('/auth/me'),
};
