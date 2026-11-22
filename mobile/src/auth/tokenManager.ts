import { refreshAsync, TokenError, type TokenResponse } from 'expo-auth-session';

import { config } from '../config';
import { clearTokens, loadTokens, saveTokens, type StoredTokens } from './tokenStorage';

// Refresh slightly early so a token never expires mid-request.
const EXPIRY_SKEW_MS = 30_000;

type Listener = () => void;
const sessionExpiredListeners = new Set<Listener>();
let refreshInFlight: Promise<string | null> | null = null;

export function onSessionExpired(listener: Listener): () => void {
  sessionExpiredListeners.add(listener);
  return () => sessionExpiredListeners.delete(listener);
}

function toStoredTokens(response: TokenResponse, previousRefreshToken: string | null): StoredTokens {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken ?? previousRefreshToken,
    expiresAt: Date.now() + (response.expiresIn ?? 60) * 1000,
  };
}

export async function storeTokenResponse(response: TokenResponse): Promise<void> {
  await saveTokens(toStoredTokens(response, null));
}

export async function expireSession(): Promise<null> {
  await clearTokens();
  sessionExpiredListeners.forEach((listener) => listener());
  return null;
}

async function refresh(refreshToken: string): Promise<string | null> {
  try {
    const response = await refreshAsync({ clientId: config.clientId, refreshToken }, config.discovery);
    const tokens = toStoredTokens(response, refreshToken);
    await saveTokens(tokens);
    return tokens.accessToken;
  } catch (error) {
    if (error instanceof TokenError) {
      // The server rejected the refresh token: the session is dead.
      console.warn(`Token refresh rejected (${error.code ?? 'unknown'})`);
      return expireSession();
    }
    throw error; // network trouble: keep the session, let the caller report it
  }
}

/**
 * Returns an access token that is valid right now, refreshing it when needed.
 * Returns null (and notifies listeners) when the user has to log in again.
 */
export async function getValidAccessToken(forceRefresh = false): Promise<string | null> {
  const tokens = await loadTokens();
  if (!tokens) return null;
  const stillValid = tokens.expiresAt - EXPIRY_SKEW_MS > Date.now();
  if (stillValid && !forceRefresh) return tokens.accessToken;
  if (!tokens.refreshToken) return expireSession();
  refreshInFlight ??= refresh(tokens.refreshToken).finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

export async function hasStoredSession(): Promise<boolean> {
  return (await loadTokens()) !== null;
}
