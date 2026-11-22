import { buildUploadForm } from '../src/services/documentService';
import { apiRequest } from '../src/services/http';
import { documentService, errorMessage, ApiError, statisticsService, tagService, validationService } from '../src/services';
import { makeDocument } from './helpers';

jest.mock('../src/services/http', () => ({ apiRequest: jest.fn() }));
const api = apiRequest as jest.Mock;

beforeEach(() => api.mockReset());

describe('documentService', () => {
  it('lists newest first and sends only the tag filter', async () => {
    api.mockResolvedValue([
      makeDocument({ id: 'old', createdAt: '2026-01-01T00:00:00Z' }),
      makeDocument({ id: 'new', createdAt: '2026-06-01T00:00:00Z' }),
    ]);
    const docs = await documentService.listDocuments({ tag: 't1' });
    expect(docs.map((d) => d.id)).toEqual(['new', 'old']);
    expect(api).toHaveBeenCalledWith('/api/v1/documents', { query: { tag: 't1' } });
  });

  it('sends untagged without a tag, even if both were given', async () => {
    api.mockResolvedValue([]);
    await documentService.listDocuments({ tag: 't1', untagged: true });
    expect(api).toHaveBeenCalledWith('/api/v1/documents', { query: { untagged: true } });
  });

  it('searches with the `query` parameter', async () => {
    api.mockResolvedValue([]);
    await documentService.searchDocuments('rent');
    expect(api).toHaveBeenCalledWith('/api/v1/documents/search', { query: { query: 'rent', tag: undefined } });
  });

  it('gets, renames, deletes and tags a document', async () => {
    api.mockResolvedValue(makeDocument());
    await documentService.getDocument('d1');
    expect(api).toHaveBeenLastCalledWith('/api/v1/documents/d1');
    await documentService.updateDocumentTitle('d1', 'New');
    expect(api).toHaveBeenLastCalledWith('/api/v1/documents/d1', { method: 'PUT', body: { title: 'New' } });
    await documentService.assignTag('d1', null);
    expect(api).toHaveBeenLastCalledWith('/api/v1/documents/d1/tag', { method: 'PUT', body: { tagId: null } });
    await documentService.deleteDocument('d1');
    expect(api).toHaveBeenLastCalledWith('/api/v1/documents/d1', { method: 'DELETE' });
  });

  it('uploads multipart form data', async () => {
    api.mockResolvedValue(makeDocument());
    await documentService.uploadDocument({ title: 'T', tagId: null }, { uri: 'file:///a.pdf', name: 'a.pdf', type: 'application/pdf' });
    const [path, options] = api.mock.calls[0];
    expect(path).toBe('/api/v1/documents');
    expect(options.method).toBe('POST');
    expect(options.formData).toBeInstanceOf(FormData);
  });

  it('declares the metadata part as JSON, as the API requires', () => {
    // Jest provides Node's FormData; use React Native's real implementation, which is what ships on device.
    const NativeFormData = jest.requireActual('react-native/Libraries/Network/FormData') as { default: typeof FormData };
    const originalFormData = global.FormData;
    global.FormData = NativeFormData.default;
    const form = buildUploadForm({ title: 'T', tagId: null }, { uri: 'file:///a.pdf', name: 'a.pdf', type: 'application/pdf' });
    global.FormData = originalFormData;
    // React Native's FormData exposes the per-part headers it will send.
    const parts = (form as unknown as { getParts: () => Array<Record<string, unknown>> }).getParts();
    const metadata = parts.find((p) => p.fieldName === 'metadata') as { string: string; headers: Record<string, string> };
    const file = parts.find((p) => p.fieldName === 'file') as { uri: string; headers: Record<string, string> };
    expect(metadata.headers['content-type']).toBe('application/json');
    expect(JSON.parse(metadata.string)).toEqual({ title: 'T', tagId: null });
    expect(file.headers['content-type']).toBe('application/pdf');
    expect(file.headers['content-disposition']).toContain('filename="a.pdf"');
    expect(file.uri).toBe('file:///a.pdf');
  });
});

describe('tagService, statisticsService, validationService', () => {
  it('maps to the right endpoints', async () => {
    api.mockResolvedValue([]);
    await tagService.listTags();
    expect(api).toHaveBeenLastCalledWith('/api/v1/tags');
    await tagService.createTag('Finance');
    expect(api).toHaveBeenLastCalledWith('/api/v1/tags', { method: 'POST', body: { name: 'Finance' } });
    await tagService.deleteTag('t1');
    expect(api).toHaveBeenLastCalledWith('/api/v1/tags/t1', { method: 'DELETE' });
    await statisticsService.getTagStatistics();
    expect(api).toHaveBeenLastCalledWith('/api/v1/statistics/tags');
    await validationService.getValidationRules();
    expect(api).toHaveBeenLastCalledWith('/api/v1/config/validation');
  });

  it('propagates conflicts unchanged', async () => {
    api.mockRejectedValue(new ApiError(409, 'Tag already exists: Finance'));
    await expect(tagService.createTag('Finance')).rejects.toThrow('Tag already exists: Finance');
  });
});

describe('errorMessage', () => {
  it('handles ApiError, Error and unknown values', () => {
    expect(errorMessage(new ApiError(409, 'Conflict!'))).toBe('Conflict!');
    expect(errorMessage(new Error('boom'))).toBe('boom');
    expect(errorMessage('x')).toBe('Something went wrong. Please try again.');
  });
});
