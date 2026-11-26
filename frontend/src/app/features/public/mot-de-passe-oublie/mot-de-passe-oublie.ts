import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthentificationService } from '../../../core/authentification/authentification.service';
import { ESPACE_PAR_ROLE } from '../../../core/authentification/modeles';
import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Logo } from '../../../shared/logo/logo';

const ADRESSE = '/api/mot-de-passe-oublie';

/**
 * Page publique « Mot de passe oublié » en trois étapes : recevoir un code par SMS, le saisir, choisir le nouveau
 * mot de passe. Puis connexion automatique avec ce nouveau mot de passe.
 * Le code est valable 10 minutes, 3 essais ; « Renvoyer » n'est possible qu'au bout d'une minute.
 */
@Component({
  selector: 'app-mot-de-passe-oublie',
  imports: [ReactiveFormsModule, RouterLink, Logo],
  templateUrl: './mot-de-passe-oublie.html',
})
export class MotDePasseOublie implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authentification = inject(AuthentificationService);

  /** Arrivé depuis la page de connexion de l'administrateur : on y revient, et on s'y connecte. */
  readonly depuisAdministration = inject(ActivatedRoute).snapshot.queryParamMap.get('espace') === 'administration';
  readonly pageDeConnexion = this.depuisAdministration ? '/administration/connexion' : '/connexion';

  readonly etape = signal<1 | 2 | 3>(1);
  private readonly formulaires = inject(FormBuilder).nonNullable;
  readonly identifiant = this.formulaires.control('', Validators.required);
  readonly code = this.formulaires.control('', [Validators.required, Validators.pattern(/^\d{6}$/)]);
  readonly nouveau = this.formulaires.group({
    nouveauMotDePasse: ['', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,}$/)]],
    confirmation: ['', Validators.required],
  });

  readonly envoiEnCours = signal(false);
  readonly information = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  /** Secondes avant de pouvoir redemander un code. */
  readonly attenteRenvoi = signal(0);
  readonly texteAttente = computed(() => {
    const secondes = this.attenteRenvoi();
    return `${Math.floor(secondes / 60)}:${String(secondes % 60).padStart(2, '0')}`;
  });
  private minuterie: ReturnType<typeof setInterval> | undefined;

  /** Robustesse simple du mot de passe, affichée sous le champ. */
  readonly robustesse = computed(() => {
    const valeur = this.motDePasseSaisi();
    const points = [valeur.length >= 8, /\d/.test(valeur), /[A-Za-z]/.test(valeur), valeur.length >= 12, /[^A-Za-z0-9]/.test(valeur)]
      .filter(Boolean).length;
    return points <= 2 ? 'faible' : points <= 3 ? 'moyenne' : 'bonne';
  });
  private readonly motDePasseSaisi = signal('');

  constructor() {
    this.nouveau.controls.nouveauMotDePasse.valueChanges.subscribe((valeur) => this.motDePasseSaisi.set(valeur));
  }

  // --- Étape 1 ---

  demanderCode(): void {
    if (this.identifiant.invalid) {
      this.identifiant.markAsTouched();
      return;
    }
    this.appeler<{ message: string; delaiRenvoiSecondes: number }>('code', { identifiant: this.identifiant.value }, (reponse) => {
      this.information.set(reponse.message);
      this.code.reset();
      this.etape.set(2);
      this.lancerAttente(reponse.delaiRenvoiSecondes);
    });
  }

  // --- Étape 2 ---

  verifierCode(): void {
    if (this.code.invalid) {
      this.code.markAsTouched();
      return;
    }
    this.appeler('verification', { identifiant: this.identifiant.value, code: this.code.value }, () => {
      this.information.set(null);
      this.etape.set(3);
    });
  }

  // --- Étape 3 ---

  reinitialiser(): void {
    const saisie = this.nouveau.getRawValue();
    if (this.nouveau.invalid) {
      this.nouveau.markAllAsTouched();
      return;
    }
    if (saisie.nouveauMotDePasse !== saisie.confirmation) {
      this.erreursServeur.set({ confirmation: 'Les deux mots de passe ne sont pas identiques.' });
      return;
    }
    const corps = { identifiant: this.identifiant.value, code: this.code.value, ...saisie };
    this.appeler('nouveau', corps, () => this.seConnecter(saisie.nouveauMotDePasse));
  }

  /** Connexion avec le nouveau mot de passe ; si elle échoue (autre page de connexion), on y renvoie. */
  private seConnecter(motDePasse: string): void {
    const espace = this.depuisAdministration ? 'ADMINISTRATION' : 'UTILISATEURS';
    this.authentification.connecter(this.identifiant.value, motDePasse, espace).subscribe({
      next: (reponse) => this.router.navigateByUrl(ESPACE_PAR_ROLE[reponse.role]),
      error: () => this.router.navigate([this.pageDeConnexion], { queryParams: { motDePasseChange: 1 } }),
    });
  }

  /** Un code expiré ou annulé ramène à l'étape 1 pour en demander un nouveau. */
  private appeler<T>(etape: string, corps: object, suite: (reponse: T) => void): void {
    this.envoiEnCours.set(true);
    this.messageErreur.set(null);
    this.erreursServeur.set({});
    this.http.post<T>(`${ADRESSE}/${etape}`, corps).subscribe({
      next: (reponse) => {
        this.envoiEnCours.set(false);
        suite(reponse);
      },
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreursServeur.set(erreursParChamp(erreur));
        this.messageErreur.set(texteErreur(erreur));
        if (erreur.error?.code === 'CODE_EXPIRE') {
          this.etape.set(this.etape() === 3 ? 1 : this.etape());
        }
      },
    });
  }

  erreur(nom: 'identifiant' | 'code' | 'nouveauMotDePasse' | 'confirmation'): string | null {
    if (this.erreursServeur()[nom]) {
      return this.erreursServeur()[nom];
    }
    const champ = nom === 'identifiant' ? this.identifiant : nom === 'code' ? this.code : this.nouveau.controls[nom];
    if (!champ.touched || champ.valid) {
      return null;
    }
    return {
      identifiant: 'Saisissez votre téléphone ou votre e-mail.',
      code: 'Le code contient 6 chiffres.',
      nouveauMotDePasse: '8 caractères minimum, dont au moins une lettre et un chiffre.',
      confirmation: 'Confirmez le nouveau mot de passe.',
    }[nom];
  }

  private lancerAttente(secondes: number): void {
    clearInterval(this.minuterie);
    this.attenteRenvoi.set(secondes);
    this.minuterie = setInterval(() => {
      this.attenteRenvoi.update((reste) => Math.max(0, reste - 1));
      if (this.attenteRenvoi() === 0) {
        clearInterval(this.minuterie);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    clearInterval(this.minuterie);
  }
}
