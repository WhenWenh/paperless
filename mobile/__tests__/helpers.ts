import type { DocumentResponse } from '../src/models';

export function makeDocument(overrides: Partial<DocumentResponse> = {}): DocumentResponse {
  return {
    id: 'doc-1',
    title: 'Invoice',
    originalFilename: 'invoice.pdf',
    contentType: 'application/pdf',
    fileSize: 2048,
    summary: null,
    createdAt: '2026-11-22T10:00:00Z',
    updatedAt: '2026-11-22T10:00:00Z',
    tagId: null,
    tagName: null,
    ...overrides,
  };
}

export function jsonResponse(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
  });
}
