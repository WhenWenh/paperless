import {
  exchangeCodeAsync,
  makeRedirectUri,
  ResponseType,
  useAuthRequest,
} from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { config } from '../config';
import { endSession } from '../services/authService';
import { onSessionExpired, hasStoredSession, storeTokenResponse } from './tokenManager';
import { clearTokens, loadTokens } from './tokenStorage';

WebBrowser.maybeCompleteAuthSession();

export type AuthStatus = 'loading' | 'signedOut' | 'signedIn';

interface AuthContextValue {
  status: AuthStatus;
  /** Last login/session problem, shown on the login screen. */
  error: string | null;
  signIn: () => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [error, setError] = useState<string | null>(null);

  const redirectUri = makeRedirectUri({ scheme: config.redirectScheme, path: 'redirect' });
  const [request, response, promptAsync] = useAuthRequest(
    {
      clientId: config.clientId,
      redirectUri,
      scopes: ['openid'],
      responseType: ResponseType.Code,
      usePKCE: true,
    },
    config.discovery,
  );

  useEffect(() => {
    hasStoredSession().then((has) => setStatus(has ? 'signedIn' : 'signedOut'));
    return onSessionExpired(() => {
      setError('Your session has expired. Please log in again.');
      setStatus('signedOut');
    });
  }, []);

  useEffect(() => {
    if (!response) return;
    if (response.type === 'success' && request?.codeVerifier) {
      exchangeCodeAsync(
        {
          clientId: config.clientId,
          code: response.params.code,
          redirectUri,
          extraParams: { code_verifier: request.codeVerifier },
        },
        config.discovery,
      )
        .then(async (tokens) => {
          await storeTokenResponse(tokens);
          setError(null);
          setStatus('signedIn');
        })
        .catch((e: unknown) => {
          console.warn('Token exchange failed', e instanceof Error ? e.message : e);
          setError('Login failed: could not complete the sign-in with the server.');
        });
    } else if (response.type === 'error') {
      console.warn('Authorization failed', response.error?.code);
      setError(`Login failed: ${response.error?.description ?? response.error?.message ?? 'unknown error'}`);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [response]);

  const signIn = useCallback(async () => {
    setError(null);
    try {
      await promptAsync();
    } catch (e) {
      console.warn('Could not open the login page', e instanceof Error ? e.message : e);
      setError('Login failed: the login page could not be opened.');
    }
  }, [promptAsync]);

  const signOut = useCallback(async () => {
    const tokens = await loadTokens();
    if (tokens?.refreshToken) await endSession(tokens.refreshToken);
    await clearTokens();
    setError(null);
    setStatus('signedOut');
  }, []);

  const value = useMemo(() => ({ status, error, signIn, signOut }), [status, error, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
