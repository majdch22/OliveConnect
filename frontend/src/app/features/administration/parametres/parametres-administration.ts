import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { startWith } from 'rxjs';

import { erreursParChamp, texteErreur } from '../../../core/erreurs/message-erreur';
import { FrequenceImport, Parametres, ParametresReponse } from '../administration.modeles';
import { AdministrationService } from '../administration.service';

/** Bornes de chaque valeur, affichées quand la saisie en sort (identiques à celles du serveur). */
const BORNES: Record<string, string> = {
  'regles.seuilAlerteMaturite': 'Entre 50 et 100 %.',
  'regles.tolerancePesee': 'Entre 100 et 150 %.',
  'regles.distanceMaximaleSignalement': 'Entre 50 et 5 000 m.',
  'regles.delaiCibleUrgences': 'Entre 1 et 168 h.',
  'securite.dureeSessionHeures': 'Entre 1 et 24 h.',
  'securite.tentativesAvantBlocage': 'Entre 3 et 10 tentatives.',
  'securite.dureeBlocageMinutes': 'Entre 5 et 60 minutes.',
  'campagne.debut': 'Indiquez la date de début.',
  'campagne.fin': 'Indiquez la date de fin.',
};

/** Les trois imports automatiques, dans l'ordre d'affichage. */
const IMPORTS = [
  { nom: 'referentiels', libelle: 'Référentiels', aide: 'Vergers, variétés, ressources, pressoirs' },
  { nom: 'meteoEtMaturite', libelle: 'Météo et maturité', aide: 'Relevés des capteurs et du service météo' },
  { nom: 'capacitePressoirs', libelle: 'Capacité des pressoirs', aide: 'Capacité de traitement publiée par les pressoirs' },
];

/**
 * Page /administration/parametres : réglages de la plateforme, appliqués dès l'enregistrement.
 * Les sections suivent celles du serveur : campagne, règles métier, imports, notifications, sécurité.
 */
@Component({
  selector: 'app-parametres-administration',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './parametres-administration.html',
})
export class ParametresAdministration implements OnInit {
  private readonly administration = inject(AdministrationService);
  private readonly constructeur = inject(FormBuilder).nonNullable;

  /** Bornes identiques à celles vérifiées par le serveur (ParametresRequete). */
  readonly formulaire = this.constructeur.group({
    campagne: this.constructeur.group({
      debut: ['', Validators.required],
      fin: ['', Validators.required],
    }),
    regles: this.constructeur.group({
      seuilAlerteMaturite: [70, [Validators.required, Validators.min(50), Validators.max(100)]],
      tolerancePesee: [110, [Validators.required, Validators.min(100), Validators.max(150)]],
      distanceMaximaleSignalement: [500, [Validators.required, Validators.min(50), Validators.max(5000)]],
      delaiCibleUrgences: [24, [Validators.required, Validators.min(1), Validators.max(168)]],
    }),
    imports: this.constructeur.group({
      referentiels: ['CHAQUE_JOUR' as FrequenceImport],
      meteoEtMaturite: ['TOUTES_LES_15_MINUTES' as FrequenceImport],
      capacitePressoirs: ['TOUTES_LES_15_MINUTES' as FrequenceImport],
    }),
    notifications: this.constructeur.group({
      smsAgriculteurs: [true],
      emailRecapitulatifQuotidien: [false],
    }),
    securite: this.constructeur.group({
      dureeSessionHeures: [8, [Validators.required, Validators.min(1), Validators.max(24)]],
      tentativesAvantBlocage: [5, [Validators.required, Validators.min(3), Validators.max(10)]],
      dureeBlocageMinutes: [15, [Validators.required, Validators.min(5), Validators.max(60)]],
    }),
  });

  readonly imports = IMPORTS;
  readonly frequences = signal<ParametresReponse['frequencesPossibles']>([]);
  readonly derniereModification = signal<{ le?: string; par?: string }>({});
  readonly chargement = signal(true);
  readonly envoiEnCours = signal(false);
  readonly messageSucces = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);
  readonly erreursServeur = signal<Record<string, string>>({});

  /** Change à chaque saisie : les boutons qui en dépendent se recalculent alors. */
  private readonly saisie = toSignal(this.formulaire.valueChanges.pipe(startWith(null)), { initialValue: null });

  private valeursEnregistrees: Parametres | null = null;

  ngOnInit(): void {
    this.administration.parametres().subscribe({
      next: (reponse) => this.afficher(reponse),
      error: (erreur: HttpErrorResponse) => {
        this.chargement.set(false);
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }

  /** Vrai dès qu'une valeur a été modifiée depuis le dernier enregistrement. */
  aDesChangements(): boolean {
    this.saisie();
    return this.valeursEnregistrees !== null && this.formulaire.dirty;
  }

  /** Message sous un champ : celui du serveur s'il y en a un, sinon la borne non respectée, sinon null. */
  erreur(chemin: string): string | null {
    const messageServeur = this.erreursServeur()[chemin];
    if (messageServeur) {
      return messageServeur;
    }
    const champ = this.formulaire.get(chemin);
    return champ && champ.invalid && (champ.touched || champ.dirty) ? (BORNES[chemin] ?? 'Valeur incorrecte.') : null;
  }

  annulerChangements(): void {
    if (this.valeursEnregistrees) {
      this.remplir(this.valeursEnregistrees);
    }
    this.erreursServeur.set({});
  }

  enregistrer(): void {
    this.messageSucces.set(null);
    this.messageErreur.set(null);
    this.erreursServeur.set({});
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      this.messageErreur.set('Certaines valeurs sont hors des limites autorisées.');
      return;
    }
    this.envoiEnCours.set(true);
    this.administration.modifierParametres(this.formulaire.getRawValue()).subscribe({
      next: (reponse) => {
        this.envoiEnCours.set(false);
        this.afficher(reponse);
        this.messageSucces.set('Paramètres enregistrés. Ils s’appliquent dès maintenant.');
      },
      error: (erreur: HttpErrorResponse) => {
        this.envoiEnCours.set(false);
        this.erreursServeur.set(erreursParChamp(erreur));
        this.messageErreur.set(texteErreur(erreur));
      },
    });
  }

  private afficher(reponse: ParametresReponse): void {
    this.frequences.set(reponse.frequencesPossibles);
    this.valeursEnregistrees = reponse.parametres;
    this.derniereModification.set({ le: reponse.parametres.modifieLe, par: reponse.parametres.modifiePar });
    this.remplir(reponse.parametres);
    this.chargement.set(false);
  }

  private remplir(parametres: Parametres): void {
    this.formulaire.reset({
      campagne: parametres.campagne,
      regles: parametres.regles,
      imports: parametres.imports,
      notifications: parametres.notifications,
      securite: parametres.securite,
    });
  }
}
