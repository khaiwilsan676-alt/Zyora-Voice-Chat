import { describe, it, expect } from 'vitest';
import { cn } from './utils';

describe('cn utility', () => {
  it('should merge class strings', () => {
    expect(cn('class1', 'class2')).toBe('class1 class2');
  });

  it('should handle undefined and null', () => {
    expect(cn('class1', undefined, 'class2', null)).toBe('class1 class2');
  });

  it('should handle objects with boolean values', () => {
    expect(cn('class1', { class2: true, class3: false })).toBe('class1 class2');
  });

  it('should handle arrays of classes', () => {
    expect(cn(['class1', 'class2'], 'class3')).toBe('class1 class2 class3');
  });

  it('should correctly merge tailwind classes and resolve conflicts', () => {
    expect(cn('p-4', 'p-8')).toBe('p-8');
    expect(cn('text-red-500', 'text-blue-500')).toBe('text-blue-500');
    expect(cn('bg-red-500 px-2', 'bg-blue-500 p-4')).toBe('bg-blue-500 p-4');
  });

  it('should handle complex mixed inputs', () => {
    expect(
      cn(
        'base-class',
        ['array-class', { 'conditional-class': true }],
        'p-4',
        undefined,
        null,
        false,
        'p-8' // overrides p-4
      )
    ).toBe('base-class array-class conditional-class p-8');
  });
});
