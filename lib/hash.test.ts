import { describe, it, expect } from 'vitest';
import { generateStableId } from './hash';

describe('generateStableId', () => {
  it('should return default ID for fallback cases', () => {
    expect(generateStableId('')).toBe('100379620');
    expect(generateStableId('N/A')).toBe('100379620');
    expect(generateStableId('User')).toBe('100379620');
  });

  it('should preserve existing numeric Account IDs', () => {
    expect(generateStableId('100002')).toBe('100002');
    expect(generateStableId('12345678')).toBe('12345678');
    expect(generateStableId('987654321')).toBe('987654321');
  });

  it('should map special Firebase UIDs correctly', () => {
    expect(generateStableId('HUSxSvQnabgU029dWYt1TUV04hd2')).toBe('100002');
    expect(generateStableId('ADqW31RGBMaosOzy0HiqexKSD7h1')).toBe('100003');
  });

  it('should deterministically generate 8-digit numeric ID for alphanumeric UIDs', () => {
    const id1 = generateStableId('some-random-firebase-uid-12345');
    const id2 = generateStableId('some-random-firebase-uid-12345');
    expect(id1).toBe(id2);
    expect(id1).toMatch(/^\d{8}$/);
  });
});
