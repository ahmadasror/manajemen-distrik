import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePagination } from './usePagination';

describe('usePagination', () => {
  it('should have default initial state (page 1, size 10, total 0)', () => {
    const { result } = renderHook(() => usePagination());
    expect(result.current.pagination.current).toBe(1);
    expect(result.current.pagination.pageSize).toBe(10);
    expect(result.current.pagination.total).toBe(0);
  });

  it('should accept custom initial page size', () => {
    const { result } = renderHook(() => usePagination(20));
    expect(result.current.pagination.pageSize).toBe(20);
  });

  it('handleTableChange should update current page and pageSize', () => {
    const { result } = renderHook(() => usePagination());
    act(() => {
      result.current.handleTableChange({ current: 3, pageSize: 25 });
    });
    expect(result.current.pagination.current).toBe(3);
    expect(result.current.pagination.pageSize).toBe(25);
  });

  it('updateTotal should update total only', () => {
    const { result } = renderHook(() => usePagination());
    act(() => {
      result.current.updateTotal(150);
    });
    expect(result.current.pagination.total).toBe(150);
    expect(result.current.pagination.current).toBe(1);
    expect(result.current.pagination.pageSize).toBe(10);
  });

  it('getParams should return 0-indexed page for API', () => {
    const { result } = renderHook(() => usePagination());
    expect(result.current.getParams()).toEqual({ page: 0, size: 10 });

    act(() => {
      result.current.handleTableChange({ current: 3, pageSize: 10 });
    });
    expect(result.current.getParams()).toEqual({ page: 2, size: 10 });
  });
});
