import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Position, SelecteurPosition } from '../../../shared/carte/selecteur-position';
import { Signalement, TYPES_SIGNALEMENT, TypeAlerte } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';

type NomChamp = 'vergerId' | 'type' | 'latitude' | 'description';

/**
 * « Signaler maturité ou problème » : type, verger, position (GPS proposé) et description.
 * Le verger choisi apparaît sur la carte en gris, pour situer la position du signalement.
 */
@Component({
  selector: 'app-formulaire-signalement',
  imports: [ReactiveFormsModule, SelecteurPosition],
  template: `
    <form [formGroup]="formulaire" (ngSubmit)="envoyer()" novalidate class="flex flex-col gap-5">
      <fieldset>
        <legend class="etiquette">Type</legend>
        <div class="mt-1 grid grid-cols-2 gap-3 sm:grid-cols-4">
          @for (type of types; track type.code) {
            <button type="button" (click)="formulaire.controls.type.setValue(type.code)" class="rounded-xl border-2 px-3 py-3 text-center"
              [class]="formulaire.controls.type.value === type.code ? 'border-olive-700 bg-olive-50' : 'border-gray-200 bg-white'"
              [attr.aria-pressed]="formulaire.controls.type.value === type.code">
              <span class="block text-2xl" aria-hidden="true">{{ type.icone }}</span>
              <span class="text-sm font-semibold text-gray-900">{{ type.libelle }}</span>
            </button>
          }
        </div>
        @if (erreur('type'); as message) { <p class="message-champ mt-1">{{ message }}</p> }
      </fieldset>

      <div class="flex flex-col gap-1">
        <label for="signalement-verger" class="etiquette">Verger concerné</label>
        <select id="signalement-verger" formControlName="vergerId" class="champ-saisie" [class.en-erreur]="erreur('vergerId')">
          <option value="" disabled>Choisissez le verger…</option>
          @for (verger of vergersActifs(); track verger.id) { <option [value]="verger.id">{{ verger.nom }}</option> }
        </select>
        @if (erreur('vergerId'); as message) { <p class="message-champ">{{ message }}</p> }
      </div>

      <div>
        <p class="etiquette">Position</p>
        <p class="mb-2 text-sm text-gray-500">Utilisez « Ma position » au verger, ou touchez la carte.</p>
        @if (vergerChoisi(); as reference) {
          <app-selecteur-position [reference]="reference" (positionChoisie)="positionChoisie($event)" />
        } @else {
          <p class="rounded-lg bg-gray-50 px-4 py-6 text-center text-sm text-gray-500">Choisissez d'abord le verger.</p>
        }
        @if (erreur('latitude'); as message) { <p class="message-champ mt-1">{{ message }}</p> }
      </div>

      <div class="flex flex-col gap-1">
        <label for="signalement-description" class="etiquette">Ce que vous observez</label>
        <textarea id="signalement-description" rows="3" formControlName="description" placeholder="Olives noires à 80 %…" class="champ-saisie" [class.en-erreur]="erreur('description')"></textarea>
        @if (erreur('description'); as message) { <p class="message-champ">{{ message }}</p> }
      </div>

      @if (messageErreur(); as message) {
        <p role="alert" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">{{ message }}</p>
      }
      <div class="flex gap-3 border-t border-gray-200 pt-5">
        <button type="submit" class="bouton-principal flex-1" [disabled]="envoiEnCours()">{{ envoiEnCours() ? 'Envoi…' : 'Envoyer le signalement' }}</button>
        <button type="button" class="bouton-secondaire" (click)="annule.emit()">Annuler</button>
      </div>
    </form>
  `,
})
export class FormulaireSignalement {
  private readonly service = inject(AgriculteurService);

  readonly envoye = output<Signalement>();
  readonly annule = output<void>();

  readonly types = TYPES_SIGNALEMENT;
  readonly vergers = toSignal(this.service.vergers(), { initialValue: [] });
  readonly vergersActifs = computed(() => this.vergers().filter((verger) => !verger.archive));

  readonly formulaire = inject(FormBuilder).group({
    type: ['' as TypeAlerte | '', Validators.required],
    vergerId: ['', Validators.required],
    latitude: [null as number | null, Validators.required],
    longitude: [null as number | null, Validators.required],
    description: ['', [Validators.required, Validators.maxLength(500)]],
  });

  private readonly vergerId = toSignal(this.formulaire.controls.vergerId.valueChanges, { initialValue: '' });
  /** Position du verger choisi, affichée comme repère sur la carte. */
  readonly vergerChoisi = computed(() => {
    const verger = this.vergers().find((candidat) => candidat.id === this.vergerId());
    return verger ? { latitude: verger.latitude, longitude: verger.longitude } : null;
  });

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

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
      vergerId: 'Choisissez le verger.',
      type: 'Choisissez le type de signalement.',
      latitude: 'Indiquez la position (GPS ou carte).',
      description: 'Décrivez ce que vous observez (500 caractères au maximum).',
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
      .signaler({
        type: saisie.type as TypeAlerte,
        vergerId: saisie.vergerId!,
        latitude: saisie.latitude!,
        longitude: saisie.longitude!,
        description: saisie.description!.trim(),
      })
      .subscribe({
        next: (signalement) => this.envoye.emit(signalement),
        error: (erreur: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          this.erreursServeur.set(erreursParChamp(erreur));
          this.messageErreur.set(texteErreur(erreur));
        },
      });
  }
}
