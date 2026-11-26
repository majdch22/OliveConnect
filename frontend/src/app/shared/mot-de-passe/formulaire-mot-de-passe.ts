import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthentificationService } from '../../core/authentification/authentification.service';
import { erreursParChamp, texteErreur } from '../../core/erreurs/message-erreur';

type NomChamp = 'motDePasseActuel' | 'nouveauMotDePasse' | 'confirmation';

/** Règle de la plateforme : 8 caractères minimum, dont au moins une lettre et un chiffre. */
const REGLE_MOT_DE_PASSE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

/**
 * Formulaire « Changer mon mot de passe » (tous les rôles).
 * L'ancien mot de passe est redemandé ; le serveur vérifie aussi la règle et la confirmation.
 */
@Component({
  selector: 'app-formulaire-mot-de-passe',
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="formulaire" (ngSubmit)="envoyer()" novalidate class="flex flex-col gap-5">
      <div class="flex flex-col gap-1">
        <label for="mdp-actuel" class="etiquette">Mot de passe actuel</label>
        <input id="mdp-actuel" type="password" formControlName="motDePasseActuel" autocomplete="current-password" class="champ-saisie" [class.en-erreur]="erreur('motDePasseActuel')" />
        @if (erreur('motDePasseActuel'); as message) { <p class="message-champ">{{ message }}</p> }
      </div>
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="flex flex-col gap-1">
          <label for="mdp-nouveau" class="etiquette">Nouveau mot de passe</label>
          <input id="mdp-nouveau" type="password" formControlName="nouveauMotDePasse" autocomplete="new-password" class="champ-saisie" [class.en-erreur]="erreur('nouveauMotDePasse')" />
          @if (erreur('nouveauMotDePasse'); as message) {
            <p class="message-champ">{{ message }}</p>
          } @else {
            <p class="text-sm text-gray-500">8 caractères minimum, dont une lettre et un chiffre.</p>
          }
        </div>
        <div class="flex flex-col gap-1">
          <label for="mdp-confirmation" class="etiquette">Confirmation</label>
          <input id="mdp-confirmation" type="password" formControlName="confirmation" autocomplete="new-password" class="champ-saisie" [class.en-erreur]="erreur('confirmation')" />
          @if (erreur('confirmation'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
      </div>
      @if (messageErreur(); as message) {
        <p role="alert" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{{ message }}</p>
      }
      @if (messageSucces(); as message) {
        <p role="status" class="rounded-lg bg-olive-50 px-4 py-3 text-sm text-olive-900">{{ message }}</p>
      }
      <button type="submit" class="bouton-principal self-start" [disabled]="envoiEnCours()">
        {{ envoiEnCours() ? 'Enregistrement…' : 'Changer le mot de passe' }}
      </button>
    </form>
  `,
})
export class FormulaireMotDePasse {
  private readonly http = inject(HttpClient);
  private readonly authentification = inject(AuthentificationService);

  /** Émis quand le mot de passe a bien été changé. */
  readonly change = output<void>();

  readonly formulaire = inject(FormBuilder).nonNullable.group({
    motDePasseActuel: ['', Validators.required],
    nouveauMotDePasse: ['', [Validators.required, Validators.pattern(REGLE_MOT_DE_PASSE)]],
    confirmation: ['', Validators.required],
  });
  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly messageSucces = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  erreur(nom: NomChamp): string | null {
    if (this.erreursServeur()[nom]) {
      return this.erreursServeur()[nom];
    }
    const champ = this.formulaire.controls[nom];
    if (!champ.touched) {
      return null;
    }
    if (nom === 'confirmation' && champ.value && champ.value !== this.formulaire.controls.nouveauMotDePasse.value) {
      return 'Les deux mots de passe ne sont pas identiques.';
    }
    if (champ.hasError('pattern')) {
      return '8 caractères minimum, dont au moins une lettre et un chiffre.';
    }
    return champ.hasError('required') ? 'Ce champ est obligatoire.' : null;
  }

  envoyer(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    this.messageSucces.set(null);
    const saisie = this.formulaire.getRawValue();
    if (this.formulaire.invalid || saisie.confirmation !== saisie.nouveauMotDePasse) {
      this.formulaire.markAllAsTouched();
      return;
    }
    this.envoiEnCours.set(true);
    this.http.put('/api/profil/mot-de-passe', saisie).subscribe({
      next: () => {
        this.envoiEnCours.set(false);
        this.formulaire.reset();
        this.messageSucces.set('Votre mot de passe a été changé.');
        this.authentification.noterMotDePasseChange();
        this.change.emit();
      },
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreursServeur.set(erreursParChamp(erreur));
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }
}
