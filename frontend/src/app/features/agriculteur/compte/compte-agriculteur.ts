import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { FormulaireMotDePasse } from '../../../shared/mot-de-passe/formulaire-mot-de-passe';
import { AgriculteurService } from '../agriculteur.service';

/** Page /agriculteur/compte : informations du compte et changement du mot de passe. */
@Component({
  selector: 'app-compte-agriculteur',
  imports: [TelephonePipe, FormulaireMotDePasse],
  template: `
    <h1 class="text-2xl font-bold text-olive-900 sm:text-3xl">Mon compte</h1>

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      @if (profil(); as compte) {
        <section class="rounded-2xl border border-gray-200 bg-white p-6">
          <h2 class="font-semibold text-gray-900">Mes informations</h2>
          <dl class="mt-5 grid grid-cols-2 gap-x-6 gap-y-5 text-sm">
            <div class="col-span-2"><dt class="text-gray-500">Nom</dt><dd class="mt-1 text-lg font-semibold text-gray-900">{{ compte.prenom }} {{ compte.nom }}</dd></div>
            <div><dt class="text-gray-500">Numéro d'adhérent</dt><dd class="mt-1 font-mono font-medium text-gray-900">{{ compte.numeroAdherent ?? '—' }}</dd></div>
            <div><dt class="text-gray-500">Délégation</dt><dd class="mt-1 font-medium text-gray-900">{{ compte.libelleDelegation ?? '—' }}</dd></div>
            <div><dt class="text-gray-500">Téléphone</dt><dd class="mt-1 font-medium text-gray-900">{{ compte.telephone | telephone }}</dd></div>
            <div><dt class="text-gray-500">E-mail</dt><dd class="mt-1 font-medium break-all text-gray-900">{{ compte.email ?? 'Non renseigné' }}</dd></div>
          </dl>
          <p class="mt-6 text-sm text-gray-500">Pour changer ces informations, contactez la coopérative.</p>
        </section>
      }
      <section class="rounded-2xl border border-gray-200 bg-white p-6">
        <h2 class="font-semibold text-gray-900">Changer mon mot de passe</h2>
        <div class="mt-5"><app-formulaire-mot-de-passe /></div>
      </section>
    </div>
  `,
})
export class CompteAgriculteur {
  readonly profil = toSignal(inject(AgriculteurService).profil());
}
