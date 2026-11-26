import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { jetonInterceptor } from './core/authentification/jeton.interceptor';

// Dates et nombres affichés à la française (25/11/2026, 3 500)
registerLocaleData(localeFr);

export const configurationApplication: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Chaque appel au serveur reçoit automatiquement le jeton d'accès
    provideHttpClient(withInterceptors([jetonInterceptor])),
    { provide: LOCALE_ID, useValue: 'fr' },
  ],
};
