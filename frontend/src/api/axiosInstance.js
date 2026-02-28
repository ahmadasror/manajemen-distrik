import axios from 'axios';
import keycloak from '../auth/keycloak';
import { generateCorrelationId } from '../utils/correlationId';

const axiosInstance = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: refresh Keycloak token then attach it + correlation ID
axiosInstance.interceptors.request.use(
  async (config) => {
    if (keycloak.authenticated) {
      try {
        // Refresh token if it expires within 30 seconds
        await keycloak.updateToken(30);
      } catch {
        keycloak.logout();
        return Promise.reject(new Error('Session expired'));
      }
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    config.headers['X-Correlation-ID'] = generateCorrelationId();
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: on 401, trigger Keycloak re-login
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      keycloak.login();
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
