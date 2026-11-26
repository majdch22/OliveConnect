import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, of, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { Pagination } from '../../../shared/ui/pagination';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import {
  Equipe,
  ETATS_RESSOURCES,
  FiltresRessources,
  Ressource,
  ResumeRessources,
  TYPES_RESSOURCES,
  TypeRessource,
} from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { FicheEquipe } from './fiche-equipe';
import { FormulaireRessource } from './formulaire-ressource';

/** Onglets : toutes les ressources, un onglet par type, puis les équipes. */
type Onglet = 'TOUTES' | TypeRessource | 'EQUIPES';

/** Contenu du panneau latéral. */
type Panneau =
  | { genre: 'ressource'; ressource: Ressource | null }
  | { genre: 'equipe'; equipe: Equipe | null };

/** Colonnes triables (noms identiques au tri côté serveur). */
const COLONNES = [
  { tri: 'CODE', libelle: 'Code' },
  { tri: 'NOM', libelle: 'Nom' },
  { tri: 'TYPE', libelle: 'Type' },
  { tri: 'ETAT', libelle: 'État' },
];

/**
 * Page /administration/ressources : tracteurs, bennes, ouvriers saisonniers, matériel et équipes d'ouvriers.
 * Ces ressources sont affectées aux tournées ; elles sont gérées au quotidien par le responsable logistique.
 */
@Component({
  selector: 'app-ressources-administration',
  imports: [Pagination, PanneauLateral, FormulaireRessource, FicheEquipe],
  templateUrl: './ressources-administration.html',
})
export class RessourcesAdministration {
  private readonly administration = inject(AdministrationService);

  readonly types = TYPES_RESSOURCES;
  readonly etats = ETATS_RESSOURCES;
  readonly colonnes = COLONNES;

  private readonly rechargement = signal(0);
  readonly messageSucces = signal<string | null>(null);
  readonly messageErreur = signal<string | null>(null);

  /** Total et disponibles par type, pour les cartes en haut de page. */
  readonly resume = toSignal(
    toObservable(this.rechargement).pipe(switchMap(() => this.administration.resumeRessources())),
    { initialValue: [] as ResumeRessources[] },
  );

  readonly equipes = toSignal(
    toObservable(this.rechargement).pipe(switchMap(() => this.administration.equipes(true))),
    { initialValue: [] as Equipe[] },
  );
  readonly equipesActives = computed(() => this.equipes().filter((equipe) => !equipe.archivee));

  // --- Onglet et filtres ---

  readonly onglet = signal<Onglet>('TOUTES');
  readonly filtres = signal<FiltresRessources>({
    texte: '', type: '', etat: '', equipeId: '', avecArchives: false, tri: 'CODE', sens: 'ASC', page: 0, taille: 20,
  });

  choisirOnglet(onglet: Onglet): void {
    this.onglet.set(onglet);
    if (onglet !== 'EQUIPES') {
      this.changerFiltres({ type: onglet === 'TOUTES' ? '' : onglet, equipeId: '' });
    }
  }

  changerFiltres(changement: Partial<FiltresRessources>): void {
    this.filtres.update((actuels) => ({ ...actuels, page: 0, ...changement }));
  }

  trierPar(tri: string): void {
    const actuels = this.filtres();
    this.changerFiltres({ tri, sens: actuels.tri === tri && actuels.sens === 'ASC' ? 'DESC' : 'ASC' });
  }

  indicateurTri(tri: string): string {
    return this.filtres().tri === tri ? (this.filtres().sens === 'ASC' ? '↑' : '↓') : '';
  }

  readonly resultat = toSignal(
    toObservable(computed(() => ({ filtres: this.filtres(), rechargement: this.rechargement() }))).pipe(
      debounceTime(200),
      switchMap(({ filtres }) =>
        this.administration.ressources(filtres).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
    ),
  );

  /** Information propre au type, affichée dans la colonne « Détail ». */
  detail(ressource: Ressource): string {
    switch (ressource.type) {
      case 'BENNE':
        return `${ressource.capaciteKg?.toLocaleString('fr-FR')} kg` + (ressource.matricule ? ` · ${ressource.matricule}` : '');
      case 'TRACTEUR':
        return ressource.matricule ?? '—';
      case 'OUVRIER':
        return ressource.nomEquipe ?? 'Sans équipe';
      case 'MATERIEL':
        return `${ressource.quantite?.toLocaleString('fr-FR')} unité(s)`;
    }
  }

  classesEtat(ressource: Ressource): string {
    return ETATS_RESSOURCES.find((etat) => etat.code === ressource.etat)?.classes ?? '';
  }

  // --- Panneau latéral ---

  readonly panneau = signal<Panneau | null>(null);
  readonly ressourceOuverte = computed(() => {
    const panneau = this.panneau();
    return panneau?.genre === 'ressource' ? panneau : null;
  });
  readonly equipeOuverte = computed(() => {
    const panneau = this.panneau();
    return panneau?.genre === 'equipe' ? panneau : null;
  });
  /** Type proposé pour un ajout : celui de l'onglet ouvert, ou tracteur depuis « Toutes ». */
  readonly typePourAjout = computed<TypeRessource>(() => {
    const onglet = this.onglet();
    return onglet === 'TOUTES' || onglet === 'EQUIPES' ? 'TRACTEUR' : onglet;
  });

  apresEnregistrementRessource(ressource: Ressource): void {
    this.panneau.set(null);
    this.messageSucces.set(`${ressource.libelleType} « ${ressource.nom} » enregistré(e) (${ressource.code}).`);
    this.rechargement.update((valeur) => valeur + 1);
  }

  changerArchivage(ressource: Ressource): void {
    this.administration.changerArchivageRessource(ressource.id, !ressource.archivee).subscribe({
      next: (modifiee) => {
        this.panneau.set(null);
        this.messageSucces.set(`« ${modifiee.nom} » ${modifiee.archivee ? 'archivé(e)' : 'restauré(e)'}.`);
        this.rechargement.update((valeur) => valeur + 1);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }

  apresChangementEquipe(equipe: Equipe): void {
    this.panneau.set(null);
    this.messageSucces.set(`Équipe « ${equipe.nom} » enregistrée (${equipe.code}).`);
    this.rechargement.update((valeur) => valeur + 1);
  }
}
