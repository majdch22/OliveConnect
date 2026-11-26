import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';

import { AuthentificationService } from '../../core/authentification/authentification.service';
import { ESPACE_PAR_ROLE, EspaceConnexion } from '../../core/authentification/modeles';
import { texteErreur } from '../../core/erreurs/message-erreur';

/**
 * Formulaire de connexion (identifiant + mot de passe), utilisé par les deux pages de connexion.
 * Après une connexion réussie, ouvre l'espace correspondant au rôle du compte.
 */
@Component({
  selector: 'app-formulaire-connexion',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './formulaire-connexion.html',
})
export class FormulaireConnexion {
  private readonly authentification = inject(AuthentificationService);
  private readonly router = inject(Router);

  /** Page sur laquelle le formulaire est affiché. */
  readonly espace = input.required<EspaceConnexion>();

  readonly formulaire = inject(FormBuilder).nonNullable.group({
    identifiant: ['', Validators.required],
    motDePasse: ['', Validators.required],
  });

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly motDePasseVisible = signal(false);

  /** Message affiché en arrivant : session expirée, ou mot de passe qui vient d'être changé. */
  readonly information = lireInformation(inject(ActivatedRoute).snapshot.queryParamMap);

  envoyer(): void {
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const { identifiant, motDePasse } = this.formulaire.getRawValue();
    this.envoiEnCours.set(true);
    this.messageErreur.set(null);

    this.authentification.connecter(identifiant, motDePasse, this.espace()).subscribe({
      next: (reponse) => this.router.navigateByUrl(ESPACE_PAR_ROLE[reponse.role]),
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }

  basculerVisibiliteMotDePasse(): void {
    this.motDePasseVisible.update((visible) => !visible);
  }

  /** Vrai si le champ est vide après que l'utilisateur l'a touché. */
  champManquant(nom: 'identifiant' | 'motDePasse'): boolean {
    const champ = this.formulaire.controls[nom];
    return champ.touched && champ.hasError('required');
  }
}

function lireInformation(parametres: ParamMap): string | null {
  if (parametres.has('sessionExpiree')) {
    return 'Session expirée : reconnectez-vous pour continuer.';
  }
  if (parametres.has('motDePasseChange')) {
    return 'Mot de passe changé : connectez-vous avec le nouveau.';
  }
  return null;
}
