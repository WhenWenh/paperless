import { formatFileSize } from './format';
import type { UploadFile, ValidationRulesResponse } from './models';

export function validateTitle(title: string, rules: ValidationRulesResponse): string | null {
  const trimmed = title.trim();
  if (!trimmed) return 'Please enter a title.';
  if (trimmed.length > rules.maxTitleLength) {
    return `The title can have at most ${rules.maxTitleLength} characters.`;
  }
  return null;
}

export function validateUpload(
  title: string,
  file: (UploadFile & { size?: number }) | null,
  rules: ValidationRulesResponse,
): string | null {
  if (!file) return 'Please take a photo or pick a file first.';
  const titleProblem = validateTitle(title, rules);
  if (titleProblem) return titleProblem;
  if (!rules.allowedContentTypes.includes(file.type)) {
    return `Files of type ${file.type || 'unknown'} are not allowed. Allowed: ${rules.allowedContentTypes.join(', ')}.`;
  }
  if (file.size !== undefined && file.size > rules.maxFileSize) {
    return `The file is ${formatFileSize(file.size)}, the maximum is ${formatFileSize(rules.maxFileSize)}.`;
  }
  return null;
}

/** Suggests a title from a filename, shortened to what the server accepts. */
export function suggestTitle(filename: string, maxLength: number): string {
  return filename.replace(/\.[^.]+$/, '').slice(0, maxLength);
}
