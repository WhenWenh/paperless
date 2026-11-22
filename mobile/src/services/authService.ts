import { config } from '../config';

/**
 * Ends the Keycloak SSO session for the given refresh token. Best effort: the
 * local session is cleared by the caller regardless of the outcome.
 */
export async function endSession(refreshToken: string): Promise<void> {
  try {
    const response = await fetch(config.discovery.endSessionEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `client_id=${encodeURIComponent(config.clientId)}&refresh_token=${encodeURIComponent(refreshToken)}`,
    });
    if (!response.ok) {
      console.warn(`Keycloak logout returned ${response.status}`);
    }
  } catch (error) {
    console.warn('Keycloak logout failed', error instanceof Error ? error.message : error);
  }
}
