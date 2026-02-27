export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const ROLES = {
  ADMIN: 'ADMIN',
  MAKER: 'MAKER',
  CHECKER: 'CHECKER',
  VIEWER: 'VIEWER',
};

export const ACTION_TYPES = {
  CREATE: 'CREATE',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE',
};

export const PENDING_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED',
};

export const PAGE_SIZE = 10;

export const TOKEN_KEY = 'accessToken';
export const REFRESH_TOKEN_KEY = 'refreshToken';
