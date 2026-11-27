import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnDestroy, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { switchMap } from 'rxjs';

import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { Signalement, TYPES_SIGNALEMENT } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';
import { FormulaireSignalement } from './formulaire-signalement';

/** Couleur de la pastille de chaque état de signalement. */
const COULEURS_STATUT: Record<Signalement['statut'], string> = {
  OUVERTE: 'bg-amber-100 text-amber-800',
  PRISE_EN_CHARGE: 'bg-sky-100 text-sky-800',
  RESOLUE: 'bg-olive-100 text-olive-900',
};

/** Page /agriculteur/signalements : les signalements de l'agriculteur et le formulaire pour en faire un. */
@Component({
  selector: 'app-signalements-agriculteur',
  imports: [DatePipe, DecimalPipe, PanneauLateral, FormulaireSignalement],
  template: `
    <header class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div>
        <h1 class="text-2xl font-bold text-olive-900 sm:text-3xl">Mes signalements</h1>
        <p class="mt-1 text-gray-600">Maturité, pluie, ravageurs… La coopérative est prévenue dès l'envoi.</p>
      </div>
      <button type="button" class="bouton-principal" (click)="formulaireOuvert.set(true)">+ Signaler</button>
    </header>

    @if (dernierEnvoi(); as envoi) {
      <div role="status" class="mt-5 rounded-lg px-4 py-3 text-sm shadow-sm" [class]="envoi.loinDuVerger ? 'bg-amber-50 text-amber-900' : 'bg-white text-olive-900'">
        ✓ Signalement {{ envoi.numero }} envoyé.
        @if (envoi.photoNonEnvoyee) { La photo n’a pas pu partir : réessayez plus tard depuis un meilleur réseau. }
        @if (envoi.loinDuVerger) {
          La position est à {{ envoi.distanceAuVergerMetres | number }} m du verger : la coopérative le vérifiera.
        }
      </div>
    }

    <ul class="mt-6 grid gap-4 lg:grid-cols-2">
      @for (signalement of signalements(); track signalement.id) {
        <li class="rounded-2xl border border-gray-200 bg-white p-5">
          <div class="flex items-start justify-between gap-3">
            <p class="text-lg font-bold text-gray-900">
              <span aria-hidden="true">{{ icone(signalement) }}</span> {{ signalement.libelleType }} · {{ signalement.nomVerger }}
            </p>
            <span class="shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold" [class]="couleurs[signalement.statut]">{{ signalement.libelleStatut }}</span>
          </div>
          <p class="mt-1 text-sm text-gray-500">
            <span class="font-mono">{{ signalement.numero }}</span> · {{ signalement.dateCreation | date: 'dd/MM/yyyy à HH:mm' }}
            @if (signalement.loinDuVerger) { · <span class="text-amber-800">à {{ signalement.distanceAuVergerMetres | number }} m du verger</span> }
          </p>
          <p class="mt-3 text-gray-700">« {{ signalement.description }} »</p>
          @if (signalement.reponseCooperative) {
            <p class="mt-2 rounded-lg bg-olive-50 px-3 py-2 text-sm text-olive-900">💬 Coopérative : {{ signalement.reponseCooperative }}</p>
          }
          @if (signalement.avecPhoto) {
            @if (photos()[signalement.id]; as adresse) {
              <img [src]="adresse" alt="Photo du signalement" class="mt-3 h-32 w-full rounded-lg object-cover sm:w-48" />
            } @else {
              <button type="button" class="mt-3 text-sm font-semibold text-olive-700 hover:underline" (click)="voirPhoto(signalement.id)">📷 Voir la photo</button>
            }
          }
        </li>
      } @empty {
        <li class="rounded-2xl border border-dashed border-gray-300 bg-white p-10 text-center text-gray-500 lg:col-span-2">
          Aucun signalement pour l'instant.
        </li>
      }
    </ul>

    @if (formulaireOuvert()) {
      <app-panneau-lateral titre="Signaler maturité ou problème" sousTitre="Avec la position GPS de votre téléphone" (fermer)="formulaireOuvert.set(false)">
        <app-formulaire-signalement (envoye)="apresEnvoi($event)" (annule)="formulaireOuvert.set(false)" />
      </app-panneau-lateral>
    }
  `,
})
export class SignalementsAgriculteur implements OnDestroy {
  private readonly service = inject(AgriculteurService);
  readonly couleurs = COULEURS_STATUT;

  private readonly rechargement = signal(0);
  readonly signalements = toSignal(toObservable(this.rechargement).pipe(switchMap(() => this.service.signalements())), {
    initialValue: [] as Signalement[],
  });

  /** Ouvert d'office quand on arrive depuis l'accueil (?nouveau=1). */
  readonly formulaireOuvert = signal(inject(ActivatedRoute).snapshot.queryParamMap.has('nouveau'));
  readonly dernierEnvoi = signal<(Signalement & { photoNonEnvoyee?: boolean }) | null>(null);

  /** Photos déjà téléchargées (adresse en mémoire du navigateur), par signalement. */
  readonly photos = signal<Record<string, string>>({});

  voirPhoto(id: string): void {
    this.service.photo(id).subscribe((image) => this.photos.update((deja) => ({ ...deja, [id]: URL.createObjectURL(image) })));
  }

  ngOnDestroy(): void {
    Object.values(this.photos()).forEach((adresse) => URL.revokeObjectURL(adresse));
  }

  icone(signalement: Signalement): string {
    return TYPES_SIGNALEMENT.find((type) => type.code === signalement.type)?.icone ?? '';
  }

  apresEnvoi(signalement: Signalement & { photoNonEnvoyee?: boolean }): void {
    this.formulaireOuvert.set(false);
    this.dernierEnvoi.set(signalement);
    this.rechargement.update((valeur) => valeur + 1);
  }
}
