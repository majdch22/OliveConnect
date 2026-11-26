import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { OptionListe, Pressoir } from '../administration.modeles';
import { AdministrationService } from '../administration.service';

type NomChamp = 'nom' | 'delegation' | 'latitude' | 'longitude' | 'capaciteJournaliereKg';

/** Message affiché quand une valeur sort des limites (identiques à celles du serveur). */
const MESSAGES: Record<NomChamp, string> = {
  nom: 'Saisissez le nom du pressoir.',
  delegation: 'Choisissez la délégation.',
  latitude: 'Latitude hors de la région de Sfax (entre 34 et 35,5).',
  longitude: 'Longitude hors de la région de Sfax (entre 9,5 et 11,5).',
  capaciteJournaliereKg: 'Entre 1 000 et 500 000 kg par jour.',
};

/** Formulaire d'ajout (pressoir absent) ou de modification d'un pressoir. */
@Component({
  selector: 'app-formulaire-pressoir',
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="formulaire" (ngSubmit)="envoyer()" novalidate class="flex flex-col gap-5">
      <div class="flex flex-col gap-1">
        <label for="pressoir-nom" class="etiquette">Nom du pressoir</label>
        <input id="pressoir-nom" formControlName="nom" class="champ-saisie" [class.en-erreur]="erreur('nom')" />
        @if (erreur('nom'); as message) { <p class="message-champ">{{ message }}</p> }
      </div>
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="flex flex-col gap-1">
          <label for="pressoir-delegation" class="etiquette">Délégation</label>
          <select id="pressoir-delegation" formControlName="delegation" class="champ-saisie" [class.en-erreur]="erreur('delegation')">
            <option value="" disabled>Choisissez…</option>
            @for (delegation of delegations(); track delegation.code) { <option [value]="delegation.code">{{ delegation.libelle }}</option> }
          </select>
          @if (erreur('delegation'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
        <div class="flex flex-col gap-1">
          <label for="pressoir-capacite" class="etiquette">Capacité (kg par jour)</label>
          <input id="pressoir-capacite" type="number" formControlName="capaciteJournaliereKg" class="champ-saisie" [class.en-erreur]="erreur('capaciteJournaliereKg')" />
          @if (erreur('capaciteJournaliereKg'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
      </div>
      <fieldset>
        <legend class="etiquette">Position</legend>
        <p class="text-sm text-gray-500">Coordonnées GPS en degrés décimaux, pour trouver le pressoir le plus proche d'un verger.</p>
        <div class="mt-2 grid gap-5 sm:grid-cols-2">
          <div class="flex flex-col gap-1">
            <label for="pressoir-latitude" class="text-sm text-gray-700">Latitude</label>
            <input id="pressoir-latitude" type="number" step="0.0001" formControlName="latitude" placeholder="34.7400" class="champ-saisie" [class.en-erreur]="erreur('latitude')" />
            @if (erreur('latitude'); as message) { <p class="message-champ">{{ message }}</p> }
          </div>
          <div class="flex flex-col gap-1">
            <label for="pressoir-longitude" class="text-sm text-gray-700">Longitude</label>
            <input id="pressoir-longitude" type="number" step="0.0001" formControlName="longitude" placeholder="10.7600" class="champ-saisie" [class.en-erreur]="erreur('longitude')" />
            @if (erreur('longitude'); as message) { <p class="message-champ">{{ message }}</p> }
          </div>
        </div>
      </fieldset>
      <div class="flex flex-col gap-1">
        <label for="pressoir-telephone" class="etiquette">Téléphone <span class="font-normal text-gray-500">(facultatif)</span></label>
        <input id="pressoir-telephone" type="tel" formControlName="telephone" class="champ-saisie" />
      </div>
      @if (messageErreur(); as message) {
        <p role="alert" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{{ message }}</p>
      }
      <div class="flex gap-3 border-t border-gray-200 pt-5">
        <button type="submit" class="bouton-principal flex-1" [disabled]="envoiEnCours()">
          {{ pressoir() ? 'Enregistrer les modifications' : 'Ajouter le pressoir' }}
        </button>
        <button type="button" class="bouton-secondaire" (click)="annule.emit()">Annuler</button>
      </div>
    </form>
  `,
})
export class FormulairePressoir implements OnInit {
  private readonly administration = inject(AdministrationService);

  readonly pressoir = input<Pressoir | null>(null);
  readonly delegations = input.required<OptionListe[]>();
  readonly enregistre = output<Pressoir>();
  readonly annule = output<void>();

  readonly formulaire = inject(FormBuilder).nonNullable.group({
    nom: ['', Validators.required],
    delegation: ['', Validators.required],
    latitude: [null as number | null, [Validators.required, Validators.min(34), Validators.max(35.5)]],
    longitude: [null as number | null, [Validators.required, Validators.min(9.5), Validators.max(11.5)]],
    capaciteJournaliereKg: [15000, [Validators.required, Validators.min(1000), Validators.max(500000)]],
    telephone: [''],
  });
  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  ngOnInit(): void {
    const pressoir = this.pressoir();
    if (pressoir) {
      this.formulaire.setValue({
        nom: pressoir.nom,
        delegation: pressoir.delegation,
        latitude: pressoir.latitude,
        longitude: pressoir.longitude,
        capaciteJournaliereKg: pressoir.capaciteJournaliereKg,
        telephone: pressoir.telephone ?? '',
      });
    }
  }

  erreur(nom: NomChamp): string | null {
    if (this.erreursServeur()[nom]) {
      return this.erreursServeur()[nom];
    }
    const champ = this.formulaire.controls[nom];
    return champ.touched && champ.invalid ? MESSAGES[nom] : null;
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
    const requete = {
      ...saisie,
      nom: saisie.nom.trim(),
      latitude: saisie.latitude as number,
      longitude: saisie.longitude as number,
      telephone: saisie.telephone.trim() === '' ? null : saisie.telephone.trim(),
    };
    this.administration.enregistrerPressoir(this.pressoir()?.id ?? null, requete).subscribe({
      next: (pressoir) => this.enregistre.emit(pressoir),
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreursServeur.set(erreursParChamp(erreur));
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }
}
