import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Role } from '../../../core/authentification/modeles';
import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { CompteAdministration, CompteEtMotDePasse, ROLES } from '../administration.modeles';
import { AdministrationService } from '../administration.service';

type NomChamp = 'nom' | 'prenom' | 'email' | 'telephone' | 'role' | 'pressoirId';

/**
 * Formulaire de création (compte absent) ou de modification (compte fourni) d'un compte.
 * - Création : uniquement des rôles du personnel ou administrateur ; un mot de passe provisoire est généré.
 * - Modification : le rôle d'un agriculteur ne peut pas changer.
 * - Un responsable du pressoir doit être rattaché à un pressoir (choisi dans la liste des pressoirs actifs).
 */
@Component({
  selector: 'app-formulaire-compte',
  imports: [ReactiveFormsModule],
  templateUrl: './formulaire-compte.html',
})
export class FormulaireCompte implements OnInit {
  private readonly administration = inject(AdministrationService);

  /** Compte à modifier, ou null pour une création. */
  readonly compte = input<CompteAdministration | null>(null);
  /** Vrai si le compte modifié est celui de l'administrateur connecté (son rôle est alors figé). */
  readonly estMonCompte = input(false);

  readonly cree = output<CompteEtMotDePasse>();
  readonly modifie = output<CompteAdministration>();
  readonly annule = output<void>();

  readonly formulaire = inject(FormBuilder).nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    telephone: ['', Validators.required],
    email: ['', Validators.email],
    role: ['' as Role | '', Validators.required],
    pressoirId: [''],
  });

  /** Pressoirs actifs, proposés quand le rôle est « Responsable du pressoir ». */
  readonly pressoirs = toSignal(
    this.administration.pressoirs({ texte: '', avecArchives: false, tri: 'NOM', sens: 'ASC' }),
    { initialValue: [] },
  );
  private readonly roleChoisi = toSignal(this.formulaire.controls.role.valueChanges, { initialValue: '' as Role | '' });
  readonly estResponsablePressoir = computed(() => this.roleChoisi() === 'RESPONSABLE_PRESSOIR');

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  readonly estCreation = computed(() => this.compte() === null);
  readonly roleFige = computed(() => this.compte()?.role === 'AGRICULTEUR' || this.estMonCompte());
  /** Les agriculteurs s'inscrivent eux-mêmes : ce rôle n'est pas proposé à la création. */
  readonly rolesProposes = computed(() =>
    this.estCreation() ? ROLES.filter((role) => role.code !== 'AGRICULTEUR') : ROLES,
  );

  ngOnInit(): void {
    const compte = this.compte();
    if (compte) {
      this.formulaire.setValue({
        prenom: compte.prenom,
        nom: compte.nom,
        telephone: compte.telephone,
        email: compte.email ?? '',
        role: compte.role,
        pressoirId: compte.pressoirId ?? '',
      });
    }
    if (this.roleFige()) {
      this.formulaire.controls.role.disable();
    }
  }

  /** Message sous un champ : celui du serveur s'il y en a un, sinon la règle non respectée. */
  erreur(nom: NomChamp): string | null {
    const messageServeur = this.erreursServeur()[nom];
    if (messageServeur) {
      return messageServeur;
    }
    const champ = this.formulaire.controls[nom];
    if (!champ.touched || champ.valid) {
      return null;
    }
    if (nom === 'pressoirId') {
      return null;
    }
    return champ.hasError('email') ? "Cette adresse e-mail n'est pas valide." : 'Ce champ est obligatoire.';
  }

  envoyer(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const saisie = this.formulaire.getRawValue();
    const requete = {
      prenom: saisie.prenom.trim(),
      nom: saisie.nom.trim(),
      telephone: saisie.telephone.trim(),
      email: saisie.email.trim() === '' ? null : saisie.email.trim(),
      role: saisie.role as Role,
      pressoirId: saisie.role === 'RESPONSABLE_PRESSOIR' && saisie.pressoirId !== '' ? saisie.pressoirId : null,
    };
    this.envoiEnCours.set(true);
    const compte = this.compte();

    if (compte) {
      this.administration.modifierCompte(compte.id, requete).subscribe({
        next: (modifie) => this.modifie.emit(modifie),
        error: (erreur: HttpErrorResponse) => this.afficherErreur(erreur),
      });
    } else {
      this.administration.creerCompte(requete).subscribe({
        next: (cree) => this.cree.emit(cree),
        error: (erreur: HttpErrorResponse) => this.afficherErreur(erreur),
      });
    }
  }

  private afficherErreur(erreur: HttpErrorResponse): void {
    this.envoiEnCours.set(false);
    this.erreursServeur.set(erreursParChamp(erreur));
    this.messageErreur.set(texteErreur(erreur));
  }
}
