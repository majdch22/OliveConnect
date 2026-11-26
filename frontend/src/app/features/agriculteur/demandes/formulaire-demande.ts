import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { of, switchMap } from 'rxjs';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { Demande, TypeRecolte } from '../agriculteur.modeles';
import { AgriculteurService } from '../agriculteur.service';

type NomChamp = 'vergerId' | 'type' | 'dateSouhaitee' | 'quantiteEstimeeKg' | 'commentaire';

/** Date au format AAAA-MM-JJ (celui des champs date), dans le fuseau du navigateur. */
function dateDuJour(decalageJours = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + decalageJours);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

/**
 * Formulaire de demande de récolte en quatre saisies : verger, type, date, quantité (+ commentaire facultatif).
 * Une récolte urgente peut être demandée pour aujourd'hui, une récolte planifiée à partir de demain.
 * La quantité estimée (superficie × rendement de la variété) est proposée en un clic.
 */
@Component({
  selector: 'app-formulaire-demande',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './formulaire-demande.html',
})
export class FormulaireDemande implements OnInit {
  private readonly service = inject(AgriculteurService);

  /** Demande à modifier, ou null pour une déclaration. */
  readonly demande = input<Demande | null>(null);
  /** Verger proposé d'office (ex. depuis la carte d'un verger). */
  readonly vergerPropose = input<string | null>(null);
  readonly enregistre = output<Demande>();
  readonly annule = output<void>();

  readonly vergers = toSignal(this.service.vergers(), { initialValue: [] });
  readonly vergersActifs = computed(() => this.vergers().filter((verger) => !verger.archive));

  readonly formulaire = inject(FormBuilder).nonNullable.group({
    vergerId: ['', Validators.required],
    type: ['PLANIFIEE' as TypeRecolte, Validators.required],
    dateSouhaitee: [dateDuJour(1), Validators.required],
    quantiteEstimeeKg: [null as number | null, [Validators.required, Validators.min(1), Validators.max(100000)]],
    commentaire: [''],
  });

  private readonly typeChoisi = toSignal(this.formulaire.controls.type.valueChanges, { initialValue: 'PLANIFIEE' as TypeRecolte });
  /** Date la plus proche autorisée : aujourd'hui pour une urgence, demain sinon. */
  readonly datePlusTot = computed(() => (this.typeChoisi() === 'URGENTE' ? dateDuJour() : dateDuJour(1)));

  /** Quantité estimée pour le verger choisi, recalculée à chaque changement de verger. */
  readonly estimation = toSignal(
    this.formulaire.controls.vergerId.valueChanges.pipe(
      switchMap((vergerId) => (vergerId ? this.service.estimation(vergerId) : of(null))),
    ),
    { initialValue: null },
  );

  readonly envoiEnCours = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  ngOnInit(): void {
    const demande = this.demande();
    if (demande) {
      this.formulaire.setValue({
        vergerId: demande.vergerId,
        type: demande.type,
        dateSouhaitee: demande.dateSouhaitee,
        quantiteEstimeeKg: demande.quantiteEstimeeKg,
        commentaire: demande.commentaire ?? '',
      });
    } else if (this.vergerPropose()) {
      this.formulaire.controls.vergerId.setValue(this.vergerPropose()!);
    }
  }

  choisirType(type: TypeRecolte): void {
    this.formulaire.controls.type.setValue(type);
  }

  utiliserEstimation(quantite: number): void {
    this.formulaire.controls.quantiteEstimeeKg.setValue(quantite);
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
      vergerId: 'Choisissez le verger.',
      type: 'Choisissez le type de récolte.',
      dateSouhaitee: 'Choisissez la date souhaitée.',
      quantiteEstimeeKg: 'Entre 1 et 100 000 kg.',
      commentaire: '300 caractères au maximum.',
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
      .enregistrerDemande(this.demande()?.id ?? null, {
        ...saisie,
        quantiteEstimeeKg: saisie.quantiteEstimeeKg!,
        commentaire: saisie.commentaire.trim() === '' ? null : saisie.commentaire.trim(),
      })
      .subscribe({
        next: (demande) => this.enregistre.emit(demande),
        error: (erreur: HttpErrorResponse) => {
          this.envoiEnCours.set(false);
          this.erreursServeur.set(erreursParChamp(erreur));
          this.messageErreur.set(texteErreur(erreur));
        },
      });
  }
}
