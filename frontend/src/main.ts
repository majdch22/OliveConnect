import { bootstrapApplication } from '@angular/platform-browser';
import { configurationApplication } from './app/app.configuration';
import { App } from './app/app';

bootstrapApplication(App, configurationApplication)
  .catch((err) => console.error(err));
