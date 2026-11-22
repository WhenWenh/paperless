import { expireSession, getValidAccessToken } from '../auth/tokenManager';
import { config } from '../config';
import type { ErrorResponse } from '../models';
import { ApiError } from './apiError';

type QueryValue = string | boolean | undefined;

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  query?: Record<string, QueryValue>;
  /** JSON body. */
  body?: unknown;
  /** Multipart body. The Content-Type header (with boundary) is set by the runtime. */
  formData?: FormData;
}

const DEFAULT_MESSAGES: Record<number, string> = {
  400: 'The request was not valid.',
  401: 'Your session is no longer valid. Please log in again.',
  403: 'You are not allowed to do that.',
  404: 'The requested item was not found.',
  409: 'That conflicts with existing data.',
  413: 'The file is too large.',
  415: 'That file type is not supported.',
};

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const params = Object.entries(query ?? {})
    .filter((entry): entry is [string, string | boolean] => entry[1] !== undefined && entry[1] !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`);
  return `${config.apiBaseUrl}${path}${params.length ? `?${params.join('&')}` : ''}`;
}

function toApiError(status: number, raw: string): ApiError {
  let body: ErrorResponse = {};
  try {
    body = JSON.parse(raw) as ErrorResponse;
  } catch {
    // Spring answers some errors (e.g. 401) without a JSON body.
  }
  const details = body.details && Object.keys(body.details).length > 0 ? body.details : undefined;
  let message = body.message?.trim() || '';
  if (details) {
    const fields = Object.entries(details)
      .map(([field, problem]) => `${field}: ${problem}`)
      .join(', ');
    message = message ? `${message} (${fields})` : fields;
  }
  return new ApiError(status, message || DEFAULT_MESSAGES[status] || `The server returned an error (${status}).`, details);
}

async function send(path: string, options: RequestOptions, token: string): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json', Authorization: `Bearer ${token}` };
  let body: BodyInit | undefined;
  if (options.formData) {
    body = options.formData;
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(options.body);
  }
  try {
    return await fetch(buildUrl(path, options.query), { method: options.method ?? 'GET', headers, body });
  } catch (error) {
    console.warn(`Request failed: ${options.method ?? 'GET'} ${path}`, error instanceof Error ? error.message : error);
    throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.');
  }
}

/**
 * The single entry point for every backend call: attaches the bearer token,
 * retries once after a 401 with a refreshed token, and turns failures into
 * {@link ApiError}.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = await getValidAccessToken();
  if (!token) throw new ApiError(401, DEFAULT_MESSAGES[401]);

  let response = await send(path, options, token);
  if (response.status === 401) {
    const refreshed = await getValidAccessToken(true);
    if (refreshed) response = await send(path, options, refreshed);
    if (!refreshed || response.status === 401) {
      await expireSession();
      throw new ApiError(401, DEFAULT_MESSAGES[401]);
    }
  }

  if (!response.ok) {
    const error = toApiError(response.status, await response.text());
    console.warn(`API error ${error.status} on ${options.method ?? 'GET'} ${path}: ${error.message}`);
    throw error;
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
