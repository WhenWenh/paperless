import { expireSession, getValidAccessToken } from '../src/auth/tokenManager';
import { ApiError } from '../src/services/apiError';
import { apiRequest } from '../src/services/http';
import { jsonResponse } from './helpers';

jest.mock('../src/auth/tokenManager', () => ({
  getValidAccessToken: jest.fn(),
  expireSession: jest.fn(async () => null),
}));

const getToken = getValidAccessToken as jest.Mock;
const fetchMock = jest.fn();

beforeEach(() => {
  jest.resetAllMocks();
  (expireSession as jest.Mock).mockResolvedValue(null);
  getToken.mockResolvedValue('token-1');
  global.fetch = fetchMock as unknown as typeof fetch;
});

describe('apiRequest', () => {
  it('attaches the bearer token and parses JSON', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, [{ id: 'a' }]));
    await expect(apiRequest('/api/v1/tags')).resolves.toEqual([{ id: 'a' }]);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8081/api/v1/tags');
    expect(init.headers.Authorization).toBe('Bearer token-1');
  });

  it('serialises query params and skips undefined values', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []));
    await apiRequest('/x', { query: { query: 'a b', tag: undefined, untagged: true } });
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/x?query=a%20b&untagged=true');
  });

  it('sends JSON bodies with a content type', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, {}));
    await apiRequest('/x', { method: 'PUT', body: { title: 't' } });
    const init = fetchMock.mock.calls[0][1];
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(init.body).toBe('{"title":"t"}');
  });

  it('does not set a content type for multipart bodies', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, {}));
    await apiRequest('/x', { method: 'POST', formData: new FormData() });
    expect(fetchMock.mock.calls[0][1].headers['Content-Type']).toBeUndefined();
  });

  it('returns undefined for 204', async () => {
    fetchMock.mockResolvedValue(jsonResponse(204));
    await expect(apiRequest('/x', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('surfaces the server message for a 409', async () => {
    fetchMock.mockResolvedValue(jsonResponse(409, { status: 409, message: 'Tag already exists: Finance' }));
    await expect(apiRequest('/api/v1/tags', { method: 'POST' })).rejects.toMatchObject({
      status: 409,
      message: 'Tag already exists: Finance',
    });
  });

  it('falls back to field details for 400 validation errors', async () => {
    fetchMock.mockResolvedValue(jsonResponse(400, { status: 400, details: { name: 'must not be blank' } }));
    const error = await apiRequest('/x').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe('name: must not be blank');
    expect((error as ApiError).details).toEqual({ name: 'must not be blank' });
  });

  it('appends details to a generic message', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, { message: 'Validation failed', details: { title: 'too long' } }),
    );
    await expect(apiRequest('/x')).rejects.toThrow('Validation failed (title: too long)');
  });

  it('never exposes a bare status code for body-less errors', async () => {
    fetchMock.mockResolvedValue(jsonResponse(404));
    await expect(apiRequest('/x')).rejects.toThrow('The requested item was not found.');
    fetchMock.mockResolvedValue(jsonResponse(500));
    await expect(apiRequest('/x')).rejects.toThrow('The server returned an error (500).');
  });

  it('refreshes the token and retries once after a 401', async () => {
    getToken.mockResolvedValueOnce('stale').mockResolvedValueOnce('fresh');
    fetchMock.mockResolvedValueOnce(jsonResponse(401)).mockResolvedValueOnce(jsonResponse(200, { ok: true }));
    await expect(apiRequest('/x')).resolves.toEqual({ ok: true });
    expect(getToken).toHaveBeenLastCalledWith(true);
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer fresh');
  });

  it('ends the session when the retry is still unauthorised', async () => {
    getToken.mockResolvedValueOnce('stale').mockResolvedValueOnce('fresh');
    fetchMock.mockResolvedValue(jsonResponse(401));
    await expect(apiRequest('/x')).rejects.toMatchObject({ status: 401 });
    expect(expireSession).toHaveBeenCalled();
  });

  it('ends the session when refreshing is impossible', async () => {
    getToken.mockResolvedValueOnce('stale').mockResolvedValueOnce(null);
    fetchMock.mockResolvedValue(jsonResponse(401));
    await expect(apiRequest('/x')).rejects.toMatchObject({ status: 401 });
    expect(expireSession).toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('fails with 401 without calling the API when there is no token', async () => {
    getToken.mockResolvedValue(null);
    await expect(apiRequest('/x')).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('reports network failures as ApiError status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Network request failed'));
    await expect(apiRequest('/x')).rejects.toMatchObject({ status: 0 });
  });
});
