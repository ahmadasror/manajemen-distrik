import { describe, it, expect } from 'vitest';
import { formatDate, formatDateTime, formatStatus, formatActionType } from './formatters';

describe('formatDate', () => {
  it('should format valid date string', () => {
    const result = formatDate('2024-01-15T10:30:00');
    expect(result).toBe('15 Jan 2024');
  });

  it('should return dash for null', () => {
    expect(formatDate(null)).toBe('-');
  });

  it('should return dash for undefined', () => {
    expect(formatDate(undefined)).toBe('-');
  });

  it('should return dash for empty string', () => {
    expect(formatDate('')).toBe('-');
  });
});

describe('formatDateTime', () => {
  it('should format valid date string with time', () => {
    const result = formatDateTime('2024-01-15T10:30:45');
    expect(result).toBe('15 Jan 2024 10:30:45');
  });

  it('should return dash for null', () => {
    expect(formatDateTime(null)).toBe('-');
  });

  it('should return dash for undefined', () => {
    expect(formatDateTime(undefined)).toBe('-');
  });
});

describe('formatStatus', () => {
  it('should return gold color for PENDING', () => {
    expect(formatStatus('PENDING')).toEqual({ color: 'gold', text: 'Pending' });
  });

  it('should return green color for APPROVED', () => {
    expect(formatStatus('APPROVED')).toEqual({ color: 'green', text: 'Approved' });
  });

  it('should return red color for REJECTED', () => {
    expect(formatStatus('REJECTED')).toEqual({ color: 'red', text: 'Rejected' });
  });

  it('should return default color for CANCELLED', () => {
    expect(formatStatus('CANCELLED')).toEqual({ color: 'default', text: 'Cancelled' });
  });

  it('should return default color with original text for unknown status', () => {
    expect(formatStatus('UNKNOWN')).toEqual({ color: 'default', text: 'UNKNOWN' });
  });
});

describe('formatActionType', () => {
  it('should return blue color for CREATE', () => {
    expect(formatActionType('CREATE')).toEqual({ color: 'blue', text: 'Create' });
  });

  it('should return orange color for UPDATE', () => {
    expect(formatActionType('UPDATE')).toEqual({ color: 'orange', text: 'Update' });
  });

  it('should return red color for DELETE', () => {
    expect(formatActionType('DELETE')).toEqual({ color: 'red', text: 'Delete' });
  });

  it('should return default color with original text for unknown type', () => {
    expect(formatActionType('UNKNOWN')).toEqual({ color: 'default', text: 'UNKNOWN' });
  });
});
