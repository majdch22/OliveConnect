import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Position, SelecteurPosition } from '../../../shared/carte/selecteur-position';
import { Verger } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';

type NomChamp = 'nom' | 'latitude' | 'delegation' | 'superficieHa' | 'nombreArbres' | 'varieteId';

/** Formulaire d'ajout (verger absent) ou de modification d'un verger, avec sa position sur la carte. */
@Component({
  selector: 'app-formulaire-verger',
  imports: [ReactiveFormsModule, SelecteurPosition],
  template: `
    <form [formGroup]="formulaire" (ngSubmit)="envoyer()" novalidate class="flex flex-col gap-5">
      <div class="flex flex-col gap-1">
        <label for="verger-nom" class="etiquette">Nom du verger</label>
        <input id="verger-nom" formControlName="nom" placeholder="Verger Nord" class="champ-saisie" [class.en-erreur]="erreur('nom')" />
        @if (erreur('nom'); as message) { <p class="message-champ">{{ message }}</p> }
      </div>

      <div>
        <p class="etiquette">Emplacement</p>
        <p class="mb-2 text-sm text-gray-500">Touchez la carte au milieu du verger, ou utilisez « Ma position » si vous y êtes.</p>
        <app-selecteur-position [positionInitiale]="positionInitiale" (positionChoisie)="positionChoisie($event)" />
        @if (erreur('latitude'); as message) { <p class="message-champ mt-1">{{ message }}</p> }
      </div>

      <div class="grid gap-5 sm:grid-cols-2">
        <div class="flex flex-col gap-1">
          <label for="verger-variete" class="etiquette">Variété principale</label>
          <select id="verger-variete" formControlName="varieteId" class="champ-saisie" [class.en-erreur]="erreur('varieteId')">
            <option value="" disabled>Choisissez…</option>
            @for (variete of varietes(); track variete.id) { <option [value]="variete.id">{{ variete.nom }}</option> }
          </select>
          @if (erreur('varieteId'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
        <div class="flex flex-col gap-1">
          <label for="verger-delegation" class="etiquette">Délégation</label>
          <select id="verger-delegation" formControlName="delegation" class="champ-saisie" [class.en-erreur]="erreur('delegation')">
            <option value="" disabled>Choisissez…</option>
            @for (delegation of delegations(); track delegation.code) { <option [value]="delegation.code">{{ delegation.libelle }}</option> }
          </select>
          @if (erreur('delegation'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
        <div class="flex flex-col gap-1">
          <label for="verger-superficie" class="etiquette">Superficie (hectares)</label>
          <input id="verger-superficie" type="number" step="0.1" formControlName="superficieHa" class="champ-saisie" [class.en-erreur]="erreur('superficieHa')" />
          @if (erreur('superficieHa'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
        <div class="flex flex-col gap-1">
          <label for="verger-arbres" class="etiquette">Nombre d'arbres</label>
          <input id="verger-arbres" type="number" formControlName="nombreArbres" class="champ-saisie" [class.en-erreur]="erreur('nombreArbres')" />
          @if (erreur('nombreArbres'); as message) { <p class="message-champ">{{ message }}</p> }
        </div>
      </div>

      @if (messageErreur(); as message) {
        <p role="alert" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{{ message }}</p>
      }
      <div class="flex gap-3 border-t border-gray-200 pt-5">
        <button type="submit" class="bouton-principal flex-1" [disabled]="envoiEnCours()">{{ verger() ? 'Enregistrer' : 'Ajouter le verger' }}</button>
        <button type="button" class="bouton-secondaire" (click)="annule.emit()">Annuler</button>
      </div>
    </form>
  `,
})
export class FormulaireVerger implements OnInit {
  private readonly service = inject(AgriculteurService);

  readonly verger = input<Verger | null>(null);
  readonly enregistre = output<Verger>();
  readonly annule = output<void>();

  readonly varietes = toSignal(this.service.varietes(), { initialValue: [] });
  readonly delegations = toSignal(this.service.delegations(), { initialValue: [] });

  readonly formulaire = inject(FormBuilder).group({
    nom: ['', Validators.required],
    latitude: [null as number | null, Validators.required],
    longitude: [null as number | null, Validators.required],
    delegation: ['', Validators.required],
    superficieHa: [null as number | null, [Validators.required, Validators.min(0.1), Validators.max(500)]],
    nombreArbres: [null as number | null, [Validators.required, Validators.min(1), Validators.max(100000)]],
    varieteId: ['', Validators.required],
  });
  positionInitiale: Position | null = null;

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  ngOnInit(): void {
    const verger = this.verger();
    if (verger) {
      this.positionInitiale = { latitude: verger.latitude, longitude: verger.longitude };
      this.formulaire.setValue({
        nom: verger.nom,
        latitude: verger.latitude,
        longitude: verger.longitude,
        delegation: verger.delegation,
        superficieHa: verger.superficieHa,
        nombreArbres: verger.nombreArbres,
        varieteId: verger.varieteId,
      });
    }
  }

  positionChoisie(position: Position): void {
    this.formulaire.patchValue({ latitude: position.latitude, longitude: position.longitude });
  }

  erreur(nom: NomChamp): string | null {
    const serveur = this.erreursServeur()[nom] ?? (nom === 'latitude' ? this.erreursServeur()['longitude'] : undefined);
    if (serveur) {
      return serveur;
    }
    const champ = this.formulaire.controls[nom];
    if (!champ.touched || champ.valid) {
      return null;
    }
    return {
      nom: 'Saisissez le nom du verger.',
      latitude: 'Placez le verger sur la carte.',
      delegation: 'Choisissez la délégation.',
      superficieHa: 'Entre 0,1 et 500 hectares.',
      nombreArbres: 'Entre 1 et 100 000 arbres.',
      varieteId: 'Choisissez la variété.',
    }[nom];
  }

  envoyer(): void {
    this.erreursServeur.set({});
    this.messageErreur.set(null);
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const saisie = this.formulaire.getRawValue();
    this.envoiEnCours.set(true);
    this.service
      .enregistrerVerger(this.verger()?.id ?? null, {
        nom: saisie.nom!.trim(),
        latitude: saisie.latitude!,
        longitude: saisie.longitude!,
        delegation: saisie.delegation!,
        superficieHa: saisie.superficieHa!,
        nombreArbres: saisie.nombreArbres!,
        varieteId: saisie.varieteId!,
      })
      .subscribe({
        next: (verger) => this.enregistre.emit(verger),
        error: (erreur: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          this.erreursServeur.set(erreursParChamp(erreur));
          this.messageErreur.set(texteErreur(erreur));
        },
      });
  }
}
