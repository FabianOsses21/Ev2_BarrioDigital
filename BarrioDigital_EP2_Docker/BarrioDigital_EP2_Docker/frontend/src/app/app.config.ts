import { demoInterceptor } from './services/demo.interceptor';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';

import { provideRouter } from '@angular/router';

import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
  withInterceptors,
} from '@angular/common/http';

import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalInterceptorConfiguration,
  MsalService,
} from '@azure/msal-angular';

import { InteractionType } from '@azure/msal-browser';

import { routes } from './app.routes';
import { MSALInstanceFactory } from './factories/msal-instance.factory';
import { environment } from '../environments/environment';

function interceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, string[] | null>();

  protectedResourceMap.set(`${environment.apiUrl}/api/*`, [environment.azure.backendScope]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideRouter(routes),

    provideHttpClient(withInterceptorsFromDi(), withInterceptors([demoInterceptor])),

    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory,
    },

    {
      provide: MSAL_GUARD_CONFIG,
      useValue: {
        interactionType: InteractionType.Redirect,

        authRequest: {
          scopes: [
            'openid',
            'profile',
            environment.azure.backendScope,
          ],
        },

        loginFailedRoute: '/login',
      },
    },

    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: interceptorConfigFactory,
    },

    ...(environment.authMode === 'entra' ? [{
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true,
    }] : []),

    MsalService,
    MsalGuard,
    MsalBroadcastService,
  ],
};