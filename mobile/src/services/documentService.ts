import type {
  AssignTagRequest,
  DocumentFilter,
  DocumentResponse,
  DocumentUploadMetadata,
  UpdateDocumentRequest,
  UploadFile,
} from '../models';
import { apiRequest } from './http';

const BASE = '/api/v1/documents';

function newestFirst(documents: DocumentResponse[]): DocumentResponse[] {
  return [...documents].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

// `tag` and `untagged=true` are rejected together by the API, so only one is sent.
function filterQuery(filter: DocumentFilter) {
  return filter.untagged ? { untagged: true } : { tag: filter.tag };
}

export async function listDocuments(filter: DocumentFilter = {}): Promise<DocumentResponse[]> {
  const documents = await apiRequest<DocumentResponse[]>(BASE, { query: filterQuery(filter) });
  return newestFirst(documents);
}

export function searchDocuments(query: string, filter: DocumentFilter = {}): Promise<DocumentResponse[]> {
  return apiRequest<DocumentResponse[]>(`${BASE}/search`, { query: { query, ...filterQuery(filter) } });
}

export function getDocument(id: string): Promise<DocumentResponse> {
  return apiRequest<DocumentResponse>(`${BASE}/${id}`);
}

export function updateDocumentTitle(id: string, title: string): Promise<DocumentResponse> {
  const body: UpdateDocumentRequest = { title };
  return apiRequest<DocumentResponse>(`${BASE}/${id}`, { method: 'PUT', body });
}

export function deleteDocument(id: string): Promise<void> {
  return apiRequest<void>(`${BASE}/${id}`, { method: 'DELETE' });
}

/** Pass `null` to remove the tag from the document. */
export function assignTag(id: string, tagId: string | null): Promise<DocumentResponse> {
  const body: AssignTagRequest = { tagId };
  return apiRequest<DocumentResponse>(`${BASE}/${id}/tag`, { method: 'PUT', body });
}

/**
 * Builds the two-part upload body. The API answers 415 unless the `metadata`
 * part is declared as application/json; React Native's FormData sends a plain
 * string part without a Content-Type, but it does honour `type` on an object
 * part that carries a `string` payload.
 */
export function buildUploadForm(metadata: DocumentUploadMetadata, file: UploadFile): FormData {
  const form = new FormData();
  form.append('metadata', { string: JSON.stringify(metadata), type: 'application/json' } as unknown as Blob);
  form.append('file', { uri: file.uri, name: file.name, type: file.type } as unknown as Blob);
  return form;
}

export function uploadDocument(metadata: DocumentUploadMetadata, file: UploadFile): Promise<DocumentResponse> {
  return apiRequest<DocumentResponse>(BASE, { method: 'POST', formData: buildUploadForm(metadata, file) });
}
