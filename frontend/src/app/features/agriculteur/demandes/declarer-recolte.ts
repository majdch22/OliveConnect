import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Demande } from '../agriculteur.modeles';
import { FormulaireDemande } from './formulaire-demande';

/** Page /agriculteur/demandes/nouvelle : « Déclarer une récolte ». Après l'envoi, la demande s'ouvre dans « Mes demandes ». */
@Component({
  selector: 'app-declarer-recolte',
  imports: [FormulaireDemande, RouterLink],
  template: `
    <a routerLink="/agriculteur/demandes" class="text-sm font-semibold text-olive-700 hover:underline">← Mes demandes</a>
    <div class="mx-auto mt-4 max-w-2xl rounded-2xl bg-white p-6 shadow-sm sm:p-8">
      <h1 class="text-2xl font-bold text-olive-900">Déclarer une récolte</h1>
      <p class="mt-1 text-gray-600">La coopérative organisera la collecte selon la maturité de votre verger.</p>
      <div class="mt-6">
        <app-formulaire-demande [vergerPropose]="vergerPropose" (enregistre)="apresEnvoi($event)" (annule)="retour()" />
      </div>
    </div>
  `,
})
export class DeclarerRecolte {
  private readonly router = inject(Router);

  /** Verger choisi d'office quand on arrive depuis la carte d'un verger (?verger=…). */
  readonly vergerPropose = inject(ActivatedRoute).snapshot.queryParamMap.get('verger');

  apresEnvoi(demande: Demande): void {
    this.router.navigate(['/agriculteur/demandes'], { queryParams: { envoyee: demande.numero } });
  }

  retour(): void {
    this.router.navigateByUrl('/agriculteur/demandes');
  }
}
