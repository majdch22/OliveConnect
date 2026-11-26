import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthentificationService } from '../../core/authentification/authentification.service';
import { ESPACE_PAR_ROLE, LIBELLES_ROLE, Role } from '../../core/authentification/modeles';
import { Logo } from '../../shared/logo/logo';

/**
 * Pages d'erreur (maquette P7) :
 * - « acces-refuse » : la page est réservée à un autre profil (403) ;
 * - « introuvable » : l'adresse n'existe pas ou le lien est expiré (404).
 * Le bouton ramène à l'espace de la personne connectée, ou à l'accueil public.
 */
@Component({
  selector: 'app-page-erreur',
  imports: [RouterLink, Logo],
  template: `
    <div class="flex min-h-screen flex-col bg-olive-50">
      <header class="border-b border-gray-200 bg-white">
        <div class="mx-auto flex h-16 max-w-6xl items-center px-4 sm:px-6">
          <a routerLink="/" aria-label="Accueil OliveConnect"><app-logo /></a>
        </div>
      </header>
      <main class="flex flex-1 items-center justify-center px-4 py-12">
        <div class="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-sm">
          <p class="text-5xl" aria-hidden="true">{{ type() === 'acces-refuse' ? '🔒' : '🧭' }}</p>
          @if (type() === 'acces-refuse') {
            <h1 class="mt-4 text-2xl font-bold text-olive-900">Accès refusé</h1>
            <p class="mt-3 text-gray-600">
              Cette page est réservée au profil « {{ profilAttendu() }} ». Vos données restent visibles uniquement par les profils autorisés.
            </p>
          } @else {
            <h1 class="mt-4 text-2xl font-bold text-olive-900">Page introuvable</h1>
            <p class="mt-3 text-gray-600">Cette adresse n'existe pas. Le lien est peut-être expiré ou mal recopié.</p>
          }
          <a [routerLink]="retour()" class="bouton-principal mt-8 w-full">
            {{ session() ? 'Retour à mon espace' : "Retour à l'accueil" }}
          </a>
        </div>
      </main>
    </div>
  `,
})
export class PageErreur {
  private readonly route = inject(ActivatedRoute).snapshot;
  /** Type d'erreur, donné par l'adresse (voir app.routes.ts). */
  readonly type = computed<'acces-refuse' | 'introuvable'>(() => this.route.data['type'] ?? 'introuvable');
  readonly session = inject(AuthentificationService).session;
  private readonly profil = this.route.queryParamMap.get('profil') as Role | null;

  readonly profilAttendu = computed(() => (this.profil && LIBELLES_ROLE[this.profil]) || 'un autre profil');
  readonly retour = computed(() => {
    const session = this.session();
    return session ? ESPACE_PAR_ROLE[session.role] : '/';
  });
}
