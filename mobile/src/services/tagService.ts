import type { CreateTagRequest, TagResponse } from '../models';
import { apiRequest } from './http';

const BASE = '/api/v1/tags';

export function listTags(): Promise<TagResponse[]> {
  return apiRequest<TagResponse[]>(BASE);
}

export function createTag(name: string): Promise<TagResponse> {
  const body: CreateTagRequest = { name };
  return apiRequest<TagResponse>(BASE, { method: 'POST', body });
}

export function deleteTag(id: string): Promise<void> {
  return apiRequest<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
