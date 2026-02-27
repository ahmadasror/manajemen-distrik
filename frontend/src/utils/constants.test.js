import { describe, it, expect } from 'vitest';
import { ROLES, ACTION_TYPES, PENDING_STATUS, PAGE_SIZE, TOKEN_KEY, REFRESH_TOKEN_KEY } from './constants';

describe('constants', () => {
  it('should define all four roles', () => {
    expect(ROLES.ADMIN).toBe('ADMIN');
    expect(ROLES.MAKER).toBe('MAKER');
    expect(ROLES.CHECKER).toBe('CHECKER');
    expect(ROLES.VIEWER).toBe('VIEWER');
    expect(Object.keys(ROLES)).toHaveLength(4);
  });

  it('should define all three action types', () => {
    expect(ACTION_TYPES.CREATE).toBe('CREATE');
    expect(ACTION_TYPES.UPDATE).toBe('UPDATE');
    expect(ACTION_TYPES.DELETE).toBe('DELETE');
    expect(Object.keys(ACTION_TYPES)).toHaveLength(3);
  });

  it('should define all four pending statuses', () => {
    expect(PENDING_STATUS.PENDING).toBe('PENDING');
    expect(PENDING_STATUS.APPROVED).toBe('APPROVED');
    expect(PENDING_STATUS.REJECTED).toBe('REJECTED');
    expect(PENDING_STATUS.CANCELLED).toBe('CANCELLED');
    expect(Object.keys(PENDING_STATUS)).toHaveLength(4);
  });

  it('should have PAGE_SIZE of 10', () => {
    expect(PAGE_SIZE).toBe(10);
  });

  it('should have correct token keys', () => {
    expect(TOKEN_KEY).toBe('accessToken');
    expect(REFRESH_TOKEN_KEY).toBe('refreshToken');
  });
});
