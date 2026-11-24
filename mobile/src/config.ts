import Constants from 'expo-constants';

interface Extra {
  apiBaseUrl?: string;
  keycloakBaseUrl?: string;
}

const extra = (Constants.expoConfig?.extra ?? {}) as Extra;

const apiBaseUrl = extra.apiBaseUrl ?? 'http://localhost:8081';
const keycloakBaseUrl = extra.keycloakBaseUrl ?? 'http://localhost:9445';
const realmUrl = `${keycloakBaseUrl}/realms/paperless/protocol/openid-connect`;

export const config = {
  apiBaseUrl,
  clientId: 'paperless-mobile',
  redirectScheme: 'paperlessmobile',
  discovery: {
    authorizationEndpoint: `${realmUrl}/auth`,
    tokenEndpoint: `${realmUrl}/token`,
    endSessionEndpoint: `${realmUrl}/logout`,
  },
} as const;
