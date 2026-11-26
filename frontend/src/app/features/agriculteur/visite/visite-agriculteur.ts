import { inject, Injectable, signal } from '@angular/core';

import { CompteService } from '../../../core/compte/compte.service';
import { EtapeVisite } from '../../../shared/visite-guidee/visite-guidee';

/** Les 5 étapes de la visite de l'espace agriculteur ; chaque cible est un attribut data-visite de la page. */
export const ETAPES_VISITE_AGRICULTEUR: EtapeVisite[] = [
  { cible: 'accueil', titre: 'Votre espace', texte: 'Tout part de cet accueil : vos actions principales, la prochaine collecte et vos notifications.' },
  { cible: 'declarer', titre: 'Déclarer une récolte', texte: 'Appuyez ici quand vos olives sont prêtes. Choisissez « Urgente » si la maturité est critique.' },
  { cible: 'signaler', titre: 'Signaler maturité ou problème', texte: 'Au verger, envoyez votre position GPS, une description et une photo à la coopérative.' },
  { cible: 'vergers', titre: 'Vos vergers', texte: 'Ajoutez vos vergers et placez-les sur la carte : ils servent à toutes vos demandes.' },
  { cible: 'aide', titre: 'Besoin d’aide ?', texte: 'La cloche vous prévient de chaque étape de vos collectes. L’aide répond aux questions fréquentes.' },
];

/**
 * État de la visite guidée de l'agriculteur, partagé entre le cadre de l'espace (qui l'affiche) et l'accueil
 * (qui la propose à la première connexion). Terminée ou passée, elle n'est plus proposée (enregistré sur le serveur).
 */
@Injectable({ providedIn: 'root' })
export class VisiteAgriculteur {
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
