import { act, render, screen, waitFor } from '@testing-library/react-native';
import { exchangeCodeAsync, useAuthRequest } from 'expo-auth-session';
import React from 'react';
import { Text } from 'react-native';

import { AuthProvider, useAuth } from '../src/auth/AuthContext';
import { expireSession } from '../src/auth/tokenManager';
import { clearTokens, loadTokens, saveTokens } from '../src/auth/tokenStorage';
import { endSession } from '../src/services/authService';

jest.mock('expo-auth-session', () => ({
  useAuthRequest: jest.fn(),
  exchangeCodeAsync: jest.fn(),
  makeRedirectUri: jest.fn(() => 'paperlessmobile://redirect'),
  refreshAsync: jest.fn(),
  TokenError: class TokenError extends Error {},
  ResponseType: { Code: 'code' },
}));
jest.mock('expo-web-browser', () => ({ maybeCompleteAuthSession: jest.fn() }));
jest.mock('../src/services/authService', () => ({ endSession: jest.fn(async () => undefined) }));

const promptAsync = jest.fn();
let authResponse: unknown = null;
let latest: ReturnType<typeof useAuth>;

function Probe() {
  latest = useAuth();
  return <Text>{`status:${latest.status} error:${latest.error ?? 'none'}`}</Text>;
}

const mountProvider = () =>
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );

beforeEach(async () => {
  jest.clearAllMocks();
  authResponse = null;
  promptAsync.mockResolvedValue({ type: 'cancel' });
  jest.mocked(useAuthRequest).mockImplementation((() => [{ codeVerifier: 'verifier' }, authResponse, promptAsync]) as never);
  await clearTokens();
});

describe('AuthProvider', () => {
  it('starts signed out without stored tokens', async () => {
    await mountProvider();
    expect(await screen.findByText('status:signedOut error:none')).toBeTruthy();
  });

  it('restores a stored session', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r', expiresAt: Date.now() + 100_000 });
    await mountProvider();
    expect(await screen.findByText('status:signedIn error:none')).toBeTruthy();
  });

  it('exchanges the code with the PKCE verifier and signs in', async () => {
    authResponse = { type: 'success', params: { code: 'the-code' } };
    jest.mocked(exchangeCodeAsync).mockResolvedValue({ accessToken: 'a', refreshToken: 'r', expiresIn: 300 } as never);
    await mountProvider();
    expect(await screen.findByText('status:signedIn error:none')).toBeTruthy();
    expect(exchangeCodeAsync).toHaveBeenCalledWith(
      expect.objectContaining({ code: 'the-code', clientId: 'paperless-mobile', extraParams: { code_verifier: 'verifier' } }),
      expect.anything(),
    );
    expect((await loadTokens())?.accessToken).toBe('a');
  });

  it('shows an error when the code exchange fails', async () => {
    authResponse = { type: 'success', params: { code: 'c' } };
    jest.mocked(exchangeCodeAsync).mockRejectedValue(new Error('bad'));
    await mountProvider();
    expect(await screen.findByText(/status:signedOut error:Login failed: could not complete/)).toBeTruthy();
  });

  it('shows an error when authorisation fails', async () => {
    authResponse = { type: 'error', error: { description: 'Access denied' } };
    await mountProvider();
    expect(await screen.findByText(/error:Login failed: Access denied/)).toBeTruthy();
  });

  it('opens the login page and reports a failure to do so', async () => {
    await mountProvider();
    await act(async () => latest.signIn());
    expect(promptAsync).toHaveBeenCalled();
    promptAsync.mockRejectedValue(new Error('no browser'));
    await act(async () => latest.signIn());
    expect(await screen.findByText(/could not be opened/)).toBeTruthy();
  });

  it('logs out on Keycloak and clears the tokens', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r', expiresAt: Date.now() + 100_000 });
    await mountProvider();
    await screen.findByText('status:signedIn error:none');
    await act(async () => latest.signOut());
    expect(endSession).toHaveBeenCalledWith('r');
    expect(await loadTokens()).toBeNull();
    await waitFor(() => expect(latest.status).toBe('signedOut'));
  });

  it('returns to login with a message when the session expires', async () => {
    await saveTokens({ accessToken: 'a', refreshToken: 'r', expiresAt: Date.now() + 100_000 });
    await mountProvider();
    await screen.findByText('status:signedIn error:none');
    await act(async () => void (await expireSession()));
    expect(await screen.findByText(/session has expired/)).toBeTruthy();
  });

  it('refuses to be used outside the provider', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    await expect(render(<Probe />)).rejects.toThrow('useAuth must be used inside <AuthProvider>');
  });
});
