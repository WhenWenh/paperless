import * as SecureStore from 'expo-secure-store';

export interface StoredTokens {
  accessToken: string;
  refreshToken: string | null;
  /** Epoch milliseconds at which the access token expires. */
  expiresAt: number;
}

// Tokens live in the platform keystore, never in AsyncStorage. They are kept
// under separate keys because SecureStore values are size-limited.
const ACCESS_KEY = 'paperless.accessToken';
const REFRESH_KEY = 'paperless.refreshToken';
const EXPIRES_KEY = 'paperless.expiresAt';

export async function saveTokens(tokens: StoredTokens): Promise<void> {
  await SecureStore.setItemAsync(ACCESS_KEY, tokens.accessToken);
  if (tokens.refreshToken) {
    await SecureStore.setItemAsync(REFRESH_KEY, tokens.refreshToken);
  } else {
    await SecureStore.deleteItemAsync(REFRESH_KEY);
  }
  await SecureStore.setItemAsync(EXPIRES_KEY, String(tokens.expiresAt));
}

export async function loadTokens(): Promise<StoredTokens | null> {
  const accessToken = await SecureStore.getItemAsync(ACCESS_KEY);
  if (!accessToken) return null;
  const refreshToken = await SecureStore.getItemAsync(REFRESH_KEY);
  const expiresAt = Number(await SecureStore.getItemAsync(EXPIRES_KEY));
  return {
    accessToken,
    refreshToken,
    expiresAt: Number.isFinite(expiresAt) ? expiresAt : 0,
  };
}

export async function clearTokens(): Promise<void> {
  await Promise.all([
    SecureStore.deleteItemAsync(ACCESS_KEY),
    SecureStore.deleteItemAsync(REFRESH_KEY),
    SecureStore.deleteItemAsync(EXPIRES_KEY),
  ]);
}
