import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { AdministrationService } from '../administration.service';

/** Page /administration : chiffres clés des comptes et dernières inscriptions à traiter. */
@Component({
  selector: 'app-accueil-administration',
  imports: [RouterLink, DatePipe, TelephonePipe],
  templateUrl: './accueil-administration.html',
})
export class AccueilAdministration {
  readonly administration = inject(AdministrationService);
  readonly session = inject(AuthentificationService).session;

  /** Les cinq inscriptions en attente les plus récentes. */
  readonly dernieresInscriptions = toSignal(
    this.administration.listerInscriptions({
      etat: 'EN_ATTENTE',
      texte: '',
      delegation: '',
      tri: 'DATE_CREATION',
      sens: 'DESC',
      page: 0,
      taille: 5,
    }),
  );

  /** Plus grand nombre de comptes pour un rôle, pour dimensionner les barres. */
  readonly maximumParRole = computed(() =>
    Math.max(1, ...(this.administration.statistiques()?.parRole ?? []).map((ligne) => ligne.nombre)),
  );
}
