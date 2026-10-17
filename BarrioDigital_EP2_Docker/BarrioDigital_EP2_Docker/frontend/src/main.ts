import { bootstrapApplication } from '@angular/platform-browser';

import { environment } from './environments/environment';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import { msalInstance } from './app/factories/msal-instance.factory';

async function iniciarAplicacion(): Promise<void> {
  if (environment.authMode === 'entra') {
  await msalInstance.initialize();

  const resultado = await msalInstance.handleRedirectPromise();

  const cuenta =
    resultado?.account ??
    msalInstance.getActiveAccount() ??
    msalInstance.getAllAccounts()[0];

  if (cuenta) {
    msalInstance.setActiveAccount(cuenta);
  }

  }
  await bootstrapApplication(App, appConfig);
}

iniciarAplicacion().catch((error) => {
  console.error('No se pudo iniciar BarrioDigital:', error);

  const root = document.querySelector('app-root');

  if (root) {
    root.textContent =
      'No se pudo iniciar la aplicación. Recarga la página e inténtalo nuevamente.';
  }
});