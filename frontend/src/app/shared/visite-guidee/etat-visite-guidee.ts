import { inject, Injectable, signal } from '@angular/core';

import { CompteService } from '../../core/compte/compte.service';

/**
 * État de la visite guidée d'un espace, partagé entre le cadre de l'espace (qui l'affiche) et son accueil
 * (qui la propose à la première connexion). Terminée ou passée, elle n'est plus proposée (enregistré sur le serveur).
 * Chaque espace en dérive son propre service (VisiteAgriculteur, VisiteCollecte…).
 */
@Injectable()
export abstract class EtatVisiteGuidee {
  private readonly compte = inject(CompteService);

  /** Vrai pendant la visite. */
  readonly enCours = signal(false);
  /** Vrai si la visite doit être proposée (jamais faite, et pas reportée pendant cette session). */
  readonly aProposer = signal(false);

  /** Relit sur le serveur si la visite a déjà été faite (à l'ouverture de l'espace, pour le compte connecté). */
  charger(): void {
    this.compte.profil().subscribe({
      next: (profil) => this.aProposer.set(!profil.visiteGuideeTerminee),
      error: () => this.aProposer.set(false),
    });
  }

  commencer(): void {
    this.aProposer.set(false);
    this.enCours.set(true);
  }

  /** « Plus tard » : ne plus la proposer pendant cette session, sans la marquer comme faite. */
  plusTard(): void {
    this.aProposer.set(false);
  }

  terminer(): void {
    this.enCours.set(false);
    this.aProposer.set(false);
    this.compte.terminerVisiteGuidee().subscribe({ error: () => undefined });
  }
}
