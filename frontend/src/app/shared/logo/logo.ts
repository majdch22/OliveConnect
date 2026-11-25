import { Component, input } from '@angular/core';

/** Logo OliveConnect : une olive stylisée suivie du nom. Version claire pour les fonds sombres. */
@Component({
  selector: 'app-logo',
  template: `
    <span class="inline-flex items-center gap-2 font-bold tracking-tight" [class.text-white]="clair()" [class.text-olive-900]="!clair()">
      <svg viewBox="0 0 32 32" class="h-8 w-8 shrink-0" aria-hidden="true">
        <path d="M16 3c6 4 9 9 9 14a9 9 0 0 1-18 0c0-5 3-10 9-14z" [attr.fill]="clair() ? '#b9d47a' : '#5b7a1e'" />
        <path d="M16 9c-2 5-2 11 0 17" [attr.stroke]="clair() ? '#2f3e12' : '#e8f0d8'" stroke-width="1.6" fill="none" stroke-linecap="round" />
      </svg>
      <span class="text-xl">OliveConnect</span>
    </span>
  `,
})
export class Logo {
  /** Vrai pour afficher le logo sur un fond foncé. */
  readonly clair = input(false);
}
