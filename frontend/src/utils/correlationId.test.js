import { describe, it, expect } from 'vitest';
import { generateCorrelationId } from './correlationId';

describe('generateCorrelationId', () => {
  it('should return a string in UUID v4 format', () => {
    const id = generateCorrelationId();
    expect(id).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
    );
  });

  it('should return unique values across multiple calls', () => {
    const ids = new Set();
    for (let i = 0; i < 100; i++) {
      ids.add(generateCorrelationId());
    }
    expect(ids.size).toBe(100);
  });

  it('should always have 4 in the third section (UUID v4)', () => {
    for (let i = 0; i < 20; i++) {
      const id = generateCorrelationId();
      const parts = id.split('-');
      expect(parts[2][0]).toBe('4');
    }
  });
});
