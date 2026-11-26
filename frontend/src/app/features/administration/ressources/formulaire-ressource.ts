import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import {
  Equipe,
  ETATS_RESSOURCES,
  EtatRessource,
  Ressource,
  TYPES_RESSOURCES,
  TypeRessource,
} from '../administration.modeles';
import { AdministrationService } from '../administration.service';

type NomChamp = 'type' | 'nom' | 'capaciteKg' | 'quantite' | 'equipeId' | 'etat';

/**
 * Formulaire d'ajout (ressource absente) ou de modification d'une ressource.
 * Seuls les champs utiles au type choisi sont affichés : capacité d'une benne, quantité d'un matériel,
 * équipe et téléphone d'un ouvrier, immatriculation d'un tracteur ou d'une benne. Le type ne change plus après l'ajout.
 */
@Component({
  selector: 'app-formulaire-ressource',
  imports: [ReactiveFormsModule],
  templateUrl: './formulaire-ressource.html',
})
export class FormulaireRessource implements OnInit {
  private readonly administration = inject(AdministrationService);

  readonly ressource = input<Ressource | null>(null);
  /** Type proposé par défaut pour un ajout (celui de l'onglet ouvert). */
  readonly typeParDefaut = input<TypeRessource>('TRACTEUR');
  readonly equipes = input.required<Equipe[]>();
  readonly enregistre = output<Ressource>();
  readonly annule = output<void>();

  readonly types = TYPES_RESSOURCES;
  readonly etats = ETATS_RESSOURCES;

  readonly formulaire = inject(FormBuilder).nonNullable.group({
    type: ['TRACTEUR' as TypeRessource, Validators.required],
    nom: ['', Validators.required],
    matricule: [''],
    capaciteKg: [2000 as number | null],
    quantite: [1 as number | null],
    equipeId: [''],
    telephone: [''],
    etat: ['DISPONIBLE' as EtatRessource, Validators.required],
  });

  /** Type choisi, pour n'afficher que les champs qui le concernent. */
  readonly type = toSignal(this.formulaire.controls.type.valueChanges, { initialValue: 'TRACTEUR' as TypeRessource });
  readonly estVehicule = computed(() => this.type() === 'TRACTEUR' || this.type() === 'BENNE');
  readonly libelleNom = computed(() =>
    ({ TRACTEUR: 'Modèle du tracteur', BENNE: 'Désignation', OUVRIER: 'Nom et prénom', MATERIEL: 'Désignation du matériel' })[
      this.type()
    ],
  );

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  ngOnInit(): void {
    const ressource = this.ressource();
    if (ressource) {
      this.formulaire.setValue({
        type: ressource.type,
        nom: ressource.nom,
        matricule: ressource.matricule ?? '',
        capaciteKg: ressource.capaciteKg,
        quantite: ressource.quantite,
        equipeId: ressource.equipeId ?? '',
        telephone: ressource.telephone ?? '',
        etat: ressource.etat,
      });
      this.formulaire.controls.type.disable();
    } else {
      this.formulaire.controls.type.setValue(this.typeParDefaut());
    }
  }

  erreur(nom: NomChamp): string | null {
    if (this.erreursServeur()[nom]) {
      return this.erreursServeur()[nom];
    }
    const champ = this.formulaire.controls[nom];
    return champ.touched && champ.invalid ? 'Ce champ est obligatoire.' : null;
  }

  envoyer(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const saisie = this.formulaire.getRawValue();
    const texteOuNull = (texte: string) => (texte.trim() === '' ? null : texte.trim());
    this.envoiEnCours.set(true);
    this.administration
      .enregistrerRessource(this.ressource()?.id ?? null, {
        type: saisie.type,
        nom: saisie.nom.trim(),
        etat: saisie.etat,
        matricule: this.estVehicule() ? texteOuNull(saisie.matricule) : null,
        capaciteKg: saisie.type === 'BENNE' ? saisie.capaciteKg : null,
        quantite: saisie.type === 'MATERIEL' ? saisie.quantite : null,
        equipeId: saisie.type === 'OUVRIER' ? texteOuNull(saisie.equipeId) : null,
        telephone: saisie.type === 'OUVRIER' ? texteOuNull(saisie.telephone) : null,
      })
      .subscribe({
        next: (ressource) => this.enregistre.emit(ressource),
        error: (erreur: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          this.erreursServeur.set(erreursParChamp(erreur));
          this.messageErreur.set(texteErreur(erreur));
        },
      });
  }
}
