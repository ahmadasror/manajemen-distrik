import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useApi } from './useApi';

// Mock antd message
vi.mock('antd', () => ({
  message: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
  },
}));

import { message } from 'antd';

describe('useApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should have initial state with null data, not loading, no error', () => {
    const mockFn = vi.fn();
    const { result } = renderHook(() => useApi(mockFn));
    expect(result.current.data).toBeNull();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('should set data on successful execute', async () => {
    const mockFn = vi.fn().mockResolvedValue({ data: { id: 1, name: 'test' } });
    const { result } = renderHook(() => useApi(mockFn));

    let returnValue;
    await act(async () => {
      returnValue = await result.current.execute('arg1');
    });

    expect(result.current.data).toEqual({ id: 1, name: 'test' });
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
    expect(returnValue).toEqual({ id: 1, name: 'test' });
    expect(mockFn).toHaveBeenCalledWith('arg1');
  });

  it('should set error and show message on failed execute', async () => {
    const mockError = { response: { data: { message: 'Server error' } } };
    const mockFn = vi.fn().mockRejectedValue(mockError);
    const { result } = renderHook(() => useApi(mockFn));

    await act(async () => {
      try {
        await result.current.execute();
      } catch {
        // expected
      }
    });

    expect(result.current.error).toBe('Server error');
    expect(result.current.loading).toBe(false);
    expect(message.error).toHaveBeenCalledWith('Server error');
  });

  it('should use err.message as fallback error message', async () => {
    const mockFn = vi.fn().mockRejectedValue(new Error('Network error'));
    const { result } = renderHook(() => useApi(mockFn));

    await act(async () => {
      try {
        await result.current.execute();
      } catch {
        // expected
      }
    });

    expect(result.current.error).toBe('Network error');
  });

  it('should rethrow the error', async () => {
    const mockFn = vi.fn().mockRejectedValue(new Error('fail'));
    const { result } = renderHook(() => useApi(mockFn));

    await expect(
      act(async () => {
        await result.current.execute();
      })
    ).rejects.toThrow('fail');
  });
});
