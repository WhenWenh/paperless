import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import { provideRouter } from '@angular/router';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import {
  provideAuth,
  withAppInitializerAuthCheck,
  authInterceptor
} from 'angular-auth-oidc-client';

import { routes } from './app.routes';
import { oidcConfig } from '@core/auth/auth.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideRouter(routes),

    provideHttpClient(
      withInterceptors([
        authInterceptor()
      ])
    ),

    provideAuth(
      { config: oidcConfig },
      withAppInitializerAuthCheck()
    )
  ]
};
