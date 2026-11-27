import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { ESPACE_PAR_ROLE } from '../../../core/authentification/modeles';
import { CompteService, PreferencesNotification, Profil } from '../../../core/compte/compte.service';
import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { FormulaireMotDePasse } from '../../../shared/mot-de-passe/formulaire-mot-de-passe';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';

type NomChamp = 'telephone' | 'email' | 'delegation' | 'motDePasse';

/**
 * Page « Mon profil » (tous les rôles) : coordonnées, préférences de notification, mot de passe,
 * visite guidée (agriculteur, responsable de collecte, responsable du pressoir) et déconnexion.
 */
@Component({
  selector: 'app-page-profil',
  imports: [ReactiveFormsModule, TelephonePipe, FormulaireMotDePasse],
  templateUrl: './page-profil.html',
})
export class PageProfil {
  private readonly service = inject(CompteService);
  private readonly router = inject(Router);
  readonly authentification = inject(AuthentificationService);

  readonly profil = signal<Profil | null>(null);
  readonly delegations = toSignal(this.service.delegations(), { initialValue: [] });
  readonly estAgriculteur = this.authentification.session()?.role === 'AGRICULTEUR';
  /** Espaces qui ont une visite guidée. */
  readonly avecVisiteGuidee = ['AGRICULTEUR', 'RESPONSABLE_COLLECTE', 'RESPONSABLE_PRESSOIR'].includes(this.authentification.session()?.role ?? '');

  readonly modification = signal(false);
  readonly formulaire = inject(FormBuilder).nonNullable.group({
    telephone: ['', Validators.required],
    email: ['', Validators.email],
    delegation: [''],
    motDePasse: ['', Validators.required],
  });
  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});
  readonly messageSucces = signal<string | null>(null);

  constructor() {
    this.service.profil().subscribe((profil) => this.profil.set(profil));
  }

  modifier(): void {
    const profil = this.profil();
    if (!profil) {
      return;
    }
    this.formulaire.reset({ telephone: profil.telephone, email: profil.email ?? '', delegation: profil.delegation ?? '', motDePasse: '' });
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    this.messageSucces.set(null);
    this.modification.set(true);
  }

  erreur(nom: NomChamp): string | null {
    if (this.erreursServeur()[nom]) {
      return this.erreursServeur()[nom];
    }
    const champ = this.formulaire.controls[nom];
    if (!champ.touched || champ.valid) {
      return null;
    }
    return {
      telephone: 'Saisissez votre numéro de téléphone.',
      email: 'Cette adresse e-mail n’est pas valide.',
      delegation: 'Choisissez votre délégation.',
      motDePasse: 'Saisissez votre mot de passe pour confirmer.',
    }[nom];
  }

  enregistrerCoordonnees(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const saisie = this.formulaire.getRawValue();
    this.envoiEnCours.set(true);
    this.service
      .changerCoordonnees({
        telephone: saisie.telephone,
        email: saisie.email.trim() === '' ? null : saisie.email.trim(),
        delegation: this.estAgriculteur ? saisie.delegation : null,
        motDePasse: saisie.motDePasse,
      })
      .subscribe({
        next: (profil) => {
          this.envoiEnCours.set(false);
          this.profil.set(profil);
          this.modification.set(false);
          this.messageSucces.set('Coordonnées enregistrées.');
        },
        error: (erreur: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          this.erreursServeur.set(erreursParChamp(erreur));
          this.messageErreur.set(texteErreur(erreur));
        },
      });
  }

  /** Les préférences s'enregistrent dès qu'on touche l'interrupteur. */
  changerPreference(cle: keyof PreferencesNotification, valeur: boolean): void {
    const profil = this.profil();
    if (!profil) {
      return;
    }
    this.service.changerPreferences({ ...profil.preferences, [cle]: valeur }).subscribe((modifie) => this.profil.set(modifie));
  }

  revoirVisiteGuidee(): void {
    const role = this.authentification.session()?.role;
    this.router.navigate([role ? ESPACE_PAR_ROLE[role] : '/'], { queryParams: { visite: 1 } });
  }
}
