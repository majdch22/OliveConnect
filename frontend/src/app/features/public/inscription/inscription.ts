import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Logo } from '../../../shared/logo/logo';
import { InscriptionService } from './inscription.service';

/** Noms des champs du formulaire, identiques à ceux attendus par le serveur. */
type NomChamp =
  | 'nom'
  | 'prenom'
  | 'cin'
  | 'telephone'
  | 'email'
  | 'delegation'
  | 'motDePasse'
  | 'confirmationMotDePasse'
  | 'conditionsAcceptees';

/** Message affiché sous un champ, pour chaque règle non respectée. */
const MESSAGES: Record<NomChamp, Record<string, string>> = {
  nom: { required: 'Saisissez votre nom.' },
  prenom: { required: 'Saisissez votre prénom.' },
  cin: { required: "Saisissez votre numéro de carte d'identité.", pattern: "La carte d'identité comporte 8 chiffres." },
  telephone: { required: 'Saisissez votre numéro de téléphone.', pattern: 'Saisissez un numéro tunisien à 8 chiffres.' },
  email: { email: "Cette adresse e-mail n'est pas valide." },
  delegation: { required: 'Choisissez votre délégation.' },
  motDePasse: {
    required: 'Choisissez un mot de passe.',
    pattern: '8 caractères minimum, dont au moins une lettre et un chiffre.',
  },
  confirmationMotDePasse: {
    required: 'Confirmez votre mot de passe.',
    differents: 'Les deux mots de passe ne sont pas identiques.',
  },
  conditionsAcceptees: { required: "Vous devez accepter les conditions d'utilisation." },
};

/**
 * Page /inscription : un agriculteur demande la création de son compte.
 * Le compte est créé « en attente de validation » ; l'administrateur l'active après vérification.
 */
@Component({
  selector: 'app-inscription',
  imports: [ReactiveFormsModule, RouterLink, Logo],
  templateUrl: './inscription.html',
})
export class Inscription {
  private readonly inscriptionService = inject(InscriptionService);
  private readonly router = inject(Router);

  /** Étapes affichées dans le panneau de gauche. */
  readonly etapes = ['Vous remplissez ce formulaire', 'La coopérative vérifie vos informations', 'Votre compte est activé'];

  readonly delegations = toSignal(this.inscriptionService.delegations(), { initialValue: [] });

  readonly formulaire = inject(FormBuilder).nonNullable.group(
    {
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      // Les espaces sont acceptés à la saisie (« 0123 4567 ») et retirés avant l'envoi
      cin: ['', [Validators.required, Validators.pattern(/^\s*(\d\s*){8}$/)]],
      telephone: ['', [Validators.required, Validators.pattern(/^\s*(\+216|00216)?[\s\d.-]{8,12}$/)]],
      email: ['', Validators.email],
      delegation: ['', Validators.required],
      motDePasse: ['', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,}$/)]],
      confirmationMotDePasse: ['', Validators.required],
      conditionsAcceptees: [false, Validators.requiredTrue],
    },
    { validators: motsDePasseIdentiques },
  );

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  /** Messages renvoyés par le serveur pour certains champs (ex. téléphone déjà utilisé). */
  readonly erreursServeur = signal<Partial<Record<NomChamp, string>>>({});

  /** Message à afficher sous un champ, ou null s'il est correct. */
  erreur(nom: NomChamp): string | null {
    const champ = this.formulaire.controls[nom];
    const messageServeur = this.erreursServeur()[nom];
    if (messageServeur) {
      return messageServeur;
    }
    if (!champ.touched) {
      return null;
    }
    if (nom === 'confirmationMotDePasse' && !champ.hasError('required') && this.formulaire.hasError('differents')) {
      return MESSAGES.confirmationMotDePasse['differents'];
    }
    const regle = Object.keys(champ.errors ?? {})[0];
    return regle ? (MESSAGES[nom][regle] ?? null) : null;
  }

  envoyer(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      this.messageErreur.set('Vérifiez les champs signalés en rouge.');
      return;
    }

    const saisie = this.formulaire.getRawValue();
    this.envoiEnCours.set(true);
    this.inscriptionService
      .inscrire({
        ...saisie,
        nom: saisie.nom.trim(),
        prenom: saisie.prenom.trim(),
        cin: saisie.cin.replace(/\s/g, ''),
        email: saisie.email.trim() === '' ? null : saisie.email.trim(),
      })
      .subscribe({
        next: (reponse) => this.router.navigateByUrl('/inscription/envoyee', { state: { inscription: reponse } }),
        error: (erreur: HttpErrorResponse) => this.afficherErreur(erreur),
      });
  }

  private afficherErreur(erreur: HttpErrorResponse): void {
    this.envoiEnCours.set(false);
    this.erreursServeur.set(erreursParChamp(erreur) as Partial<Record<NomChamp, string>>);
    this.messageErreur.set(texteErreur(erreur));
  }
}

/** Le mot de passe et sa confirmation doivent être identiques. */
function motsDePasseIdentiques(groupe: AbstractControl): ValidationErrors | null {
  const motDePasse = groupe.get('motDePasse')?.value;
  const confirmation = groupe.get('confirmationMotDePasse')?.value;
  return confirmation && motDePasse !== confirmation ? { differents: true } : null;
}
