import { refreshAsync, TokenError } from 'expo-auth-session';

import { endSession } from '../src/services/authService';
import { getValidAccessToken, hasStoredSession, onSessionExpired, storeTokenResponse } from '../src/auth/tokenManager';
import { clearTokens, loadTokens, saveTokens } from '../src/auth/tokenStorage';

jest.mock('expo-auth-session', () => {
  class TokenError extends Error {
    code = 'invalid_grant';
  }
  return { refreshAsync: jest.fn(), TokenError };
});
const refresh = refreshAsync as jest.Mock;

beforeEach(async () => {
  refresh.mockReset();
  await clearTokens();
});

const inFuture = (ms: number) => Date.now() + ms;

describe('tokenStorage', () => {
  it('round-trips and clears tokens', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r', expiresAt: 123 });
    expect(await loadTokens()).toEqual({ accessToken: 'a', refreshToken: 'r', expiresAt: 123 });
    await saveTokens({ accessToken: 'a2', refreshToken: null, expiresAt: 5 });
    expect((await loadTokens())?.refreshToken).toBeNull();
    await clearTokens();
    expect(await loadTokens()).toBeNull();
  });
});

describe('tokenManager', () => {
  it('returns null without a session', async () => {
    expect(await getValidAccessToken()).toBeNull();
    expect(await hasStoredSession()).toBe(false);
  });

  it('returns a still-valid token without refreshing', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r', expiresAt: inFuture(120_000) });
    expect(await getValidAccessToken()).toBe('a');
    expect(refresh).not.toHaveBeenCalled();
  });

  it('refreshes an expired token and stores the result', async () => {
    await saveTokens({ accessToken: 'old', refreshToken: 'r1', expiresAt: inFuture(-1000) });
    refresh.mockResolvedValue({ accessToken: 'new', refreshToken: 'r2', expiresIn: 300 });
    expect(await getValidAccessToken()).toBe('new');
    expect(await loadTokens()).toMatchObject({ accessToken: 'new', refreshToken: 'r2' });
  });

  it('shares one refresh between concurrent callers', async () => {
    await saveTokens({ accessToken: 'old', refreshToken: 'r1', expiresAt: inFuture(-1000) });
    refresh.mockResolvedValue({ accessToken: 'new', expiresIn: 300 });
    const results = await Promise.all([getValidAccessToken(), getValidAccessToken()]);
    expect(results).toEqual(['new', 'new']);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect((await loadTokens())?.refreshToken).toBe('r1'); // kept when the server sends none
  });

  it('forces a refresh on request', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r', expiresAt: inFuture(120_000) });
    refresh.mockResolvedValue({ accessToken: 'b', expiresIn: 300 });
    expect(await getValidAccessToken(true)).toBe('b');
  });

  it('ends the session when the refresh token is rejected', async () => {
    const listener = jest.fn();
    const off = onSessionExpired(listener);
    await saveTokens({ accessToken: 'old', refreshToken: 'dead', expiresAt: inFuture(-1000) });
    refresh.mockRejectedValue(new (TokenError as unknown as new (message: string) => Error)('invalid_grant'));
    expect(await getValidAccessToken()).toBeNull();
    expect(listener).toHaveBeenCalled();
    expect(await loadTokens()).toBeNull();
    off();
  });

  it('ends the session when an expired token has no refresh token', async () => {
    await saveTokens({ accessToken: 'old', refreshToken: null, expiresAt: inFuture(-1000) });
    expect(await getValidAccessToken()).toBeNull();
  });

  it('keeps the session on network failure', async () => {
    await saveTokens({ accessToken: 'old', refreshToken: 'r', expiresAt: inFuture(-1000) });
    refresh.mockRejectedValue(new TypeError('Network request failed'));
    await expect(getValidAccessToken()).rejects.toThrow('Network request failed');
    expect(await loadTokens()).not.toBeNull();
  });

  it('stores a login token response', async () => {
    await storeTokenResponse({ accessToken: 'a', refreshToken: 'r', expiresIn: 60 } as never);
    expect(await hasStoredSession()).toBe(true);
  });
});

describe('authService.endSession', () => {
  it('posts the refresh token to the logout endpoint', async () => {
    const fetchMock = jest.fn().mockResolvedValue({ ok: true, status: 204 });
    global.fetch = fetchMock as unknown as typeof fetch;
    await endSession('r 1');
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/realms/paperless/protocol/openid-connect/logout');
    expect(init.body).toBe('client_id=paperless-mobile&refresh_token=r%201');
  });

  it('never throws', async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error('offline')) as unknown as typeof fetch;
    await expect(endSession('r')).resolves.toBeUndefined();
    global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 400 }) as unknown as typeof fetch;
    await expect(endSession('r')).resolves.toBeUndefined();
  });
});
