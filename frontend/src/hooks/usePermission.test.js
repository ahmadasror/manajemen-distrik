import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { usePermission } from './usePermission';

// Mock useAuth
vi.mock('../auth/AuthContext', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../auth/AuthContext';

function mockAuth(roles, userId = 1) {
  useAuth.mockReturnValue({
    user: { id: userId, username: 'test', roles },
    hasRole: (role) => roles.includes(role),
    hasAnyRole: (...r) => r.some((role) => roles.includes(role)),
  });
}

describe('usePermission', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('canCreate', () => {
    it('should be true for ADMIN', () => {
      mockAuth(['ADMIN']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCreate).toBe(true);
    });

    it('should be true for MAKER', () => {
      mockAuth(['MAKER']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCreate).toBe(true);
    });

    it('should be false for CHECKER', () => {
      mockAuth(['CHECKER']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCreate).toBe(false);
    });

    it('should be false for VIEWER', () => {
      mockAuth(['VIEWER']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCreate).toBe(false);
    });
  });

  describe('canApprove', () => {
    it('should be true for ADMIN', () => {
      mockAuth(['ADMIN']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canApprove).toBe(true);
    });

    it('should be true for CHECKER', () => {
      mockAuth(['CHECKER']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canApprove).toBe(true);
    });

    it('should be false for MAKER', () => {
      mockAuth(['MAKER']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canApprove).toBe(false);
    });
  });

  describe('isAdmin', () => {
    it('should be true for ADMIN', () => {
      mockAuth(['ADMIN']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.isAdmin).toBe(true);
    });

    it('should be false for non-ADMIN', () => {
      mockAuth(['MAKER']);
      const { result } = renderHook(() => usePermission());
      expect(result.current.isAdmin).toBe(false);
    });
  });

  describe('canViewAll', () => {
    it('should be true for any role', () => {
      for (const role of ['ADMIN', 'MAKER', 'CHECKER', 'VIEWER']) {
        mockAuth([role]);
        const { result } = renderHook(() => usePermission());
        expect(result.current.canViewAll).toBe(true);
      }
    });
  });

  describe('canApprovePending', () => {
    it('should return true if checker is different from maker', () => {
      mockAuth(['CHECKER'], 2);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canApprovePending({ makerId: 1, status: 'PENDING' })).toBe(true);
    });

    it('should return false if checker is same as maker', () => {
      mockAuth(['CHECKER'], 1);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canApprovePending({ makerId: 1, status: 'PENDING' })).toBe(false);
    });

    it('should return false if user is not CHECKER or ADMIN', () => {
      mockAuth(['MAKER'], 2);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canApprovePending({ makerId: 1, status: 'PENDING' })).toBe(false);
    });
  });

  describe('canCancelPending', () => {
    it('should return true if user is maker and status is PENDING', () => {
      mockAuth(['MAKER'], 1);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCancelPending({ makerId: 1, status: 'PENDING' })).toBe(true);
    });

    it('should return false if user is not maker', () => {
      mockAuth(['MAKER'], 2);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCancelPending({ makerId: 1, status: 'PENDING' })).toBe(false);
    });

    it('should return false if status is not PENDING', () => {
      mockAuth(['MAKER'], 1);
      const { result } = renderHook(() => usePermission());
      expect(result.current.canCancelPending({ makerId: 1, status: 'APPROVED' })).toBe(false);
    });
  });
});
