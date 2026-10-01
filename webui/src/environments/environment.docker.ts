// environments/environment.docker.ts
export const environment = {
  apiUrl: '/api',

  oidc: {
    authority: 'http://localhost:9445/realms/paperless',
    clientId: 'paperless-webui',
    redirectUrl: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    scope: 'openid profile email',
    responseType: 'code'
  }
};
