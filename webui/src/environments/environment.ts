// environments/environment.ts
export const environment = {
  apiUrl: '',

  oidc: {
    authority: 'http://localhost:9445/realms/paperless',
    clientId: 'paperless-webui',
    redirectUrl: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    scope: 'openid profile email',
    responseType: 'code'
  }
};
