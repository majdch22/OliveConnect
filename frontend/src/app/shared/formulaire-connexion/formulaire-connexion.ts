import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthentificationService } from '../../core/authentification/authentification.service';
import { ReponseErreur, ESPACE_PAR_ROLE, EspaceConnexion } from '../../core/authentification/modeles';

/**
 * Formulaire de connexion (identifiant + mot de passe), utilisé par les deux pages de connexion.
 * Après une connexion réussie, ouvre l'espace correspondant au rôle du compte.
 */
@Component({
  selector: 'app-formulaire-connexion',
  imports: [ReactiveFormsModule],
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
        this.messageErreur.set(messagePour(erreur));
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

/** Message affiché sous le formulaire : celui du serveur s'il en donne un. */
function messagePour(erreur: HttpErrorResponse): string {
  if (erreur.status === 0) {
    return 'Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.';
  }
  const corps = erreur.error as Partial<ReponseErreur> | null;
  return corps?.message ?? 'Une erreur inattendue est survenue. Réessayez dans un instant.';
}
