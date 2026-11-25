import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Logo } from '../../../shared/logo/logo';
import { InscriptionReponse } from './inscription.service';

/**
 * Page /inscription/envoyee : confirmation après l'envoi du formulaire d'inscription.
 * Les informations (prénom, téléphone masqué) arrivent avec la navigation depuis la page d'inscription.
 */
@Component({
  selector: 'app-inscription-envoyee',
  imports: [RouterLink, Logo],
  template: `
    <div class="flex min-h-screen flex-col bg-olive-50">
      <header class="border-b border-gray-200 bg-white">
        <div class="mx-auto flex h-16 max-w-6xl items-center px-6">
          <a routerLink="/" aria-label="Accueil OliveConnect"><app-logo /></a>
        </div>
      </header>

      <main class="flex flex-1 items-center justify-center px-6 py-12">
        <div class="w-full max-w-xl rounded-2xl bg-white p-8 text-center shadow-sm lg:p-12">
          <span class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-olive-100 text-3xl text-olive-700" aria-hidden="true">✓</span>
          <h1 class="mt-6 text-3xl font-bold text-olive-900">Demande envoyée</h1>
          <p class="mt-3 text-gray-600">
            {{ inscription ? 'Merci ' + inscription.prenom + '.' : 'Merci.' }}
            La coopérative va vérifier vos informations avant d'activer votre compte.
          </p>

          <ol class="mt-10 grid grid-cols-3 gap-2 text-sm">
            <li class="flex flex-col items-center gap-2">
              <span class="flex h-9 w-9 items-center justify-center rounded-full bg-olive-700 font-bold text-white">✓</span>
              <span class="font-semibold text-olive-900">Envoyée</span>
            </li>
            <li class="flex flex-col items-center gap-2">
              <span class="flex h-9 w-9 items-center justify-center rounded-full border-2 border-olive-700 font-bold text-olive-700">2</span>
              <span class="font-semibold text-olive-900">Vérification</span>
            </li>
            <li class="flex flex-col items-center gap-2">
              <span class="flex h-9 w-9 items-center justify-center rounded-full bg-gray-200 font-bold text-gray-500">3</span>
              <span class="text-gray-500">Compte actif</span>
            </li>
          </ol>

          @if (inscription) {
            <p class="mt-10 rounded-lg bg-olive-50 p-4 text-sm text-gray-700">
              Une fois votre compte activé, connectez-vous avec le numéro <strong>{{ inscription.telephoneMasque }}</strong>
              et le mot de passe que vous avez choisi.
            </p>
          }

          <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <a routerLink="/" class="bouton-secondaire">Retour à l'accueil</a>
            <a routerLink="/connexion" class="bouton-principal">Aller à la connexion</a>
          </div>
        </div>
      </main>
    </div>
  `,
})
export class InscriptionEnvoyee {
  /** Données transmises par la page d'inscription (absentes si la page est ouverte directement). */
  readonly inscription = (history.state?.inscription as InscriptionReponse | undefined) ?? null;
}
