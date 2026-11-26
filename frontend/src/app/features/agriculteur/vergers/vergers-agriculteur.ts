import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { StatutRecolte, Verger } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';
import { CarteVergers } from './carte-vergers';
import { FormulaireVerger } from './formulaire-verger';

/** Couleur de la pastille de chaque statut de récolte. */
const COULEURS_RECOLTE: Record<StatutRecolte, string> = {
  NON_MUR: 'bg-gray-100 text-gray-700',
  MUR: 'bg-olive-100 text-olive-900',
  EN_RECOLTE: 'bg-amber-100 text-amber-800',
  RECOLTE: 'bg-olive-700 text-white',
};

/**
 * Page /agriculteur/vergers : les vergers de l'agriculteur, en liste ou sur une carte ; ajout, modification,
 * archivage, et suppression d'un verger jamais utilisé.
 */
@Component({
  selector: 'app-vergers-agriculteur',
  imports: [DecimalPipe, RouterLink, PanneauLateral, FormulaireVerger, CarteVergers],
  templateUrl: './vergers-agriculteur.html',
})
export class VergersAgriculteur {
  private readonly service = inject(AgriculteurService);
  readonly couleurs = COULEURS_RECOLTE;

  private readonly rechargement = signal(0);
  readonly vergers = toSignal(toObservable(this.rechargement).pipe(switchMap(() => this.service.vergers())), {
    initialValue: [] as Verger[],
  });
  readonly afficherArchives = signal(false);
  readonly vue = signal<'liste' | 'carte'>('liste');
  readonly vergersAffiches = computed(() =>
    this.vergers().filter((verger) => this.afficherArchives() || !verger.archive),
  );
  readonly nombreArchives = computed(() => this.vergers().filter((verger) => verger.archive).length);

  /** Panneau : null = fermé, 'nouveau' = ajout, sinon le verger modifié. */
  readonly panneau = signal<Verger | 'nouveau' | null>(null);
  readonly vergerModifie = computed(() => {
    const panneau = this.panneau();
    return panneau === 'nouveau' ? null : panneau;
  });
  readonly messageSucces = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);

  apresEnregistrement(verger: Verger): void {
    this.panneau.set(null);
    this.messageSucces.set(`Verger « ${verger.nom} » enregistré (${verger.code}).`);
    this.rechargement.update((valeur) => valeur + 1);
  }

  /** Un verger qui a déjà des demandes ou des signalements est refusé par le serveur (409) : le message invite à l'archiver. */
  supprimer(verger: Verger): void {
    this.messageErreur.set(null);
    this.service.supprimerVerger(verger.id).subscribe({
      next: () => {
        this.panneau.set(null);
        this.messageSucces.set(`Verger « ${verger.nom} » supprimé.`);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => {
        this.panneau.set(null);
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }

  changerArchivage(verger: Verger): void {
    this.service.changerArchivageVerger(verger.id, !verger.archive).subscribe({
      next: (modifie) => {
        this.panneau.set(null);
        this.messageSucces.set(`Verger « ${modifie.nom} » ${modifie.archive ? 'archivé' : 'restauré'}.`);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }
}
