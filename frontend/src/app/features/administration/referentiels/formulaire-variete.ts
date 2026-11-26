import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Variete } from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { MOIS } from './mois';

/** Formulaire d'ajout (variété absente) ou de modification d'une variété d'olive. */
@Component({
  selector: 'app-formulaire-variete',
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="formulaire" (ngSubmit)="envoyer()" novalidate class="flex flex-col gap-5">
      <div class="flex flex-col gap-1">
        <label for="variete-nom" class="etiquette">Nom de la variété</label>
        <input id="variete-nom" formControlName="nom" class="champ-saisie" [class.en-erreur]="erreur('nom')" />
        @if (erreur('nom'); as message) { <p class="message-champ">{{ message }}</p> }
      </div>
      <div class="flex flex-col gap-1">
        <label for="variete-rendement" class="etiquette">Rendement moyen (kg d'olives par hectare)</label>
        <input id="variete-rendement" type="number" formControlName="rendementMoyenKgParHectare" class="champ-saisie" [class.en-erreur]="erreur('rendementMoyenKgParHectare')" />
        @if (erreur('rendementMoyenKgParHectare'); as message) {
          <p class="message-champ">{{ message }}</p>
        } @else {
          <p class="text-sm text-gray-500">Sert à proposer une quantité estimée à l'agriculteur et à comparer les rendements réels.</p>
        }
      </div>
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="flex flex-col gap-1">
          <label for="variete-debut" class="etiquette">Début de la récolte</label>
          <select id="variete-debut" formControlName="moisDebutRecolte" class="champ-saisie">
            @for (mois of mois; track mois.numero) { <option [ngValue]="mois.numero">{{ mois.nom }}</option> }
          </select>
        </div>
        <div class="flex flex-col gap-1">
          <label for="variete-fin" class="etiquette">Fin de la récolte</label>
          <select id="variete-fin" formControlName="moisFinRecolte" class="champ-saisie">
            @for (mois of mois; track mois.numero) { <option [ngValue]="mois.numero">{{ mois.nom }}</option> }
          </select>
        </div>
      </div>
      @if (messageErreur(); as message) {
        <p role="alert" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{{ message }}</p>
      }
      <div class="flex gap-3 border-t border-gray-200 pt-5">
        <button type="submit" class="bouton-principal flex-1" [disabled]="envoiEnCours()">
          {{ variete() ? 'Enregistrer les modifications' : 'Ajouter la variété' }}
        </button>
        <button type="button" class="bouton-secondaire" (click)="annule.emit()">Annuler</button>
      </div>
    </form>
  `,
})
export class FormulaireVariete implements OnInit {
  private readonly administration = inject(AdministrationService);

  readonly variete = input<Variete | null>(null);
  readonly enregistre = output<Variete>();
  readonly annule = output<void>();

  readonly mois = MOIS;
  readonly formulaire = inject(FormBuilder).nonNullable.group({
    nom: ['', Validators.required],
    rendementMoyenKgParHectare: [3000, [Validators.required, Validators.min(500), Validators.max(20000)]],
    moisDebutRecolte: [11],
    moisFinRecolte: [2],
  });
  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  ngOnInit(): void {
    const variete = this.variete();
    if (variete) {
      this.formulaire.setValue({
        nom: variete.nom,
        rendementMoyenKgParHectare: variete.rendementMoyenKgParHectare,
        moisDebutRecolte: variete.moisDebutRecolte,
        moisFinRecolte: variete.moisFinRecolte,
      });
    }
  }

  erreur(nom: 'nom' | 'rendementMoyenKgParHectare'): string | null {
    const champ = this.formulaire.controls[nom];
    if (this.erreursServeur()[nom]) {
      return this.erreursServeur()[nom];
    }
    if (!champ.touched || champ.valid) {
      return null;
    }
    return nom === 'nom' ? 'Saisissez le nom de la variété.' : 'Entre 500 et 20 000 kg par hectare.';
  }

  envoyer(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    this.envoiEnCours.set(true);
    const saisie = this.formulaire.getRawValue();
    this.administration.enregistrerVariete(this.variete()?.id ?? null, { ...saisie, nom: saisie.nom.trim() }).subscribe({
      next: (variete) => this.enregistre.emit(variete),
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreursServeur.set(erreursParChamp(erreur));
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }
}
