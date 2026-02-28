import axiosInstance from './axiosInstance';

export const authApi = {
  me: () => axiosInstance.get('/auth/me'),
};
