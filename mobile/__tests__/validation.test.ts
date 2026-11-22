import { formatDateTime, formatFileSize } from '../src/format';
import type { ValidationRulesResponse } from '../src/models';
import { suggestTitle, validateTitle, validateUpload } from '../src/validation';

const rules: ValidationRulesResponse = { maxTitleLength: 20, maxFileSize: 1000, allowedContentTypes: ['application/pdf'] };
const pdf = { uri: 'file:///a.pdf', name: 'a.pdf', type: 'application/pdf' };

describe('validateTitle', () => {
  it('rejects blank and too long titles using the server limit', () => {
    expect(validateTitle('  ', rules)).toMatch(/enter a title/);
    expect(validateTitle('x'.repeat(21), rules)).toMatch(/at most 20/);
    expect(validateTitle('x'.repeat(20), rules)).toBeNull();
    expect(validateTitle('xxxxx', { ...rules, maxTitleLength: 3 })).toMatch(/at most 3/);
  });
});

describe('validateUpload', () => {
  it('requires a file', () => expect(validateUpload('T', null, rules)).toMatch(/pick a file/));
  it('checks the title before the file', () => expect(validateUpload('', pdf, rules)).toMatch(/title/));
  it('rejects disallowed content types', () =>
    expect(validateUpload('T', { ...pdf, type: 'image/jpeg' }, rules)).toMatch(/image\/jpeg.*not allowed/));
  it('rejects oversized files', () => expect(validateUpload('T', { ...pdf, size: 2000 }, rules)).toMatch(/maximum/));
  it('accepts a valid upload, with or without a known size', () => {
    expect(validateUpload('T', pdf, rules)).toBeNull();
    expect(validateUpload('T', { ...pdf, size: 1000 }, rules)).toBeNull();
  });
});

describe('helpers', () => {
  it('suggests a title within the limit', () => expect(suggestTitle('a-very-long-filename.pdf', 8)).toBe('a-very-l'));
  it('formats sizes', () => {
    expect(formatFileSize(10)).toBe('10 B');
    expect(formatFileSize(2048)).toBe('2.0 KB');
    expect(formatFileSize(5 * 1024 * 1024)).toBe('5.0 MB');
  });
  it('formats dates and tolerates garbage', () => {
    expect(formatDateTime('not a date')).toBe('not a date');
    expect(formatDateTime('2026-11-22T10:00:00Z')).not.toBe('2026-11-22T10:00:00Z');
  });
});
