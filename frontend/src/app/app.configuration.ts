import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { jetonInterceptor } from './core/authentification/jeton.interceptor';

export const configurationApplication: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Chaque appel au serveur reçoit automatiquement le jeton d'accès
    provideHttpClient(withInterceptors([jetonInterceptor])),
  ],
};
