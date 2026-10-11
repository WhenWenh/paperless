import {
  OpenIdConfiguration
} from 'angular-auth-oidc-client';

import {
  environment
} from '@env/environment';


export const oidcConfig: OpenIdConfiguration = {

  authority:
  environment.oidc.authority,

  clientId:
  environment.oidc.clientId,

  redirectUrl:
  environment.oidc.redirectUrl,

  postLogoutRedirectUri:
  environment.oidc.postLogoutRedirectUri,

  scope:
  environment.oidc.scope,

  responseType:
  environment.oidc.responseType,

  silentRenew:
    true,

  useRefreshToken:
    true,

  secureRoutes: [

    environment.apiUrl

  ]

};
