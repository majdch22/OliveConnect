import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, of, switchMap, tap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { BadgeStatut } from '../../../shared/ui/badge-statut';
import { Pagination } from '../../../shared/ui/pagination';
import { PanneauLateral } from '../../../shared/ui/panneau-lateral';
import { TelephonePipe } from '../../../shared/pipes/telephone.pipe';
import { CompteAdministration, CompteEtMotDePasse, FiltresComptes, ROLES, STATUTS, TriComptes } from '../administration.modeles';
import { AdministrationService } from '../administration.service';
import { FicheCompte } from './fiche-compte';
import { FormulaireCompte } from './formulaire-compte';
import { MotDePasseProvisoire } from './mot-de-passe-provisoire';

/** Contenu du panneau latéral : la fiche d'un compte, ou la création d'un compte. */
type Panneau =
  | { type: 'fiche'; compte: CompteAdministration }
  | { type: 'creation' }
  | { type: 'compte-cree'; resultat: CompteEtMotDePasse };

/** Colonnes triables : un clic sur l'en-tête trie, un second clic inverse le sens. */
const COLONNES_TRIABLES: { tri: TriComptes; libelle: string }[] = [
  { tri: 'NOM', libelle: 'Nom' },
  { tri: 'ROLE', libelle: 'Rôle' },
  { tri: 'STATUT', libelle: 'État' },
  { tri: 'DERNIERE_CONNEXION', libelle: 'Dernière connexion' },
  { tri: 'DATE_CREATION', libelle: 'Créé le' },
];

const FILTRES_INITIAUX: FiltresComptes = {
  texte: '',
  role: '',
  statut: '',
  delegation: '',
  tri: 'NOM',
  sens: 'ASC',
  page: 0,
  taille: 20,
};

/**
 * Page /administration/utilisateurs : tous les comptes de la plateforme.
 * Recherche, filtres (rôle, état, délégation), tri par colonne et pagination, exécutés par le serveur.
 */
@Component({
  selector: 'app-utilisateurs-administration',
  imports: [DatePipe, BadgeStatut, Pagination, PanneauLateral, FicheCompte, FormulaireCompte, MotDePasseProvisoire, TelephonePipe],
  templateUrl: './utilisateurs-administration.html',
})
export class UtilisateursAdministration {
  private readonly administration = inject(AdministrationService);

  readonly roles = ROLES;
  readonly statuts = STATUTS;
  readonly colonnes = COLONNES_TRIABLES;
  readonly delegations = toSignal(this.administration.delegations(), { initialValue: [] });

  // --- Liste ---

  readonly filtres = signal<FiltresComptes>(FILTRES_INITIAUX);
  private readonly rechargement = signal(0);
  readonly chargement = signal(true);
  readonly messageErreur = signal<string | null>(null);

  /** Recharge la liste à chaque changement de filtre (avec un court délai pendant la frappe). */
  readonly resultat = toSignal(
    toObservable(computed(() => ({ filtres: this.filtres(), rechargement: this.rechargement() }))).pipe(
      debounceTime(250),
      tap(() => this.chargement.set(true)),
      switchMap(({ filtres }) =>
        this.administration.listerComptes(filtres).pipe(
          catchError((erreur: HttpErrorResponse) => {
            this.messageErreur.set(texteErreur(erreur));
            return of(null);
          }),
        ),
      ),
      tap(() => this.chargement.set(false)),
    ),
  );

  readonly filtresActifs = computed(() => {
    const filtres = this.filtres();
    return filtres.texte !== '' || filtres.role !== '' || filtres.statut !== '' || filtres.delegation !== '';
  });

  changerFiltres(changement: Partial<FiltresComptes>): void {
    this.filtres.update((actuels) => ({ ...actuels, page: 0, ...changement }));
  }

  effacerFiltres(): void {
    this.changerFiltres({ texte: '', role: '', statut: '', delegation: '' });
  }

  /** Premier clic : tri par cette colonne (dates : plus récentes d'abord). Clic suivant : sens inverse. */
  trierPar(tri: TriComptes): void {
    const actuels = this.filtres();
    if (actuels.tri === tri) {
      this.changerFiltres({ sens: actuels.sens === 'ASC' ? 'DESC' : 'ASC' });
    } else {
      const estUneDate = tri === 'DATE_CREATION' || tri === 'DERNIERE_CONNEXION';
      this.changerFiltres({ tri, sens: estUneDate ? 'DESC' : 'ASC' });
    }
  }

  /**
   * Sur téléphone, les en-têtes du tableau ne sont pas affichés : le tri se choisit dans une liste.
   * Chaque choix associe une colonne et un sens (ex. « NOM:ASC »).
   */
  readonly ordresTelephone = [
    { code: 'NOM:ASC', libelle: 'Nom (A → Z)' },
    { code: 'NOM:DESC', libelle: 'Nom (Z → A)' },
    { code: 'DATE_CREATION:DESC', libelle: 'Plus récents' },
    { code: 'DATE_CREATION:ASC', libelle: 'Plus anciens' },
    { code: 'DERNIERE_CONNEXION:DESC', libelle: 'Dernière connexion' },
    { code: 'ROLE:ASC', libelle: 'Rôle' },
    { code: 'STATUT:ASC', libelle: 'État' },
  ];
  readonly ordreTelephoneChoisi = computed(() => `${this.filtres().tri}:${this.filtres().sens}`);

  choisirOrdreTelephone(code: string): void {
    const [tri, sens] = code.split(':') as [TriComptes, 'ASC' | 'DESC'];
    this.changerFiltres({ tri, sens });
  }

  /** Flèche affichée à côté de la colonne triée. */
  indicateurTri(tri: TriComptes): string {
    const actuels = this.filtres();
    if (actuels.tri !== tri) {
      return '';
    }
    return actuels.sens === 'ASC' ? '↑' : '↓';
  }

  // --- Panneau latéral ---

  readonly panneau = signal<Panneau | null>(null);

  /** Ce qu'affiche le panneau, une valeur par cas (null quand le cas ne s'applique pas). */
  readonly compteOuvert = computed(() => {
    const panneau = this.panneau();
    return panneau?.type === 'fiche' ? panneau.compte : null;
  });
  readonly creationOuverte = computed(() => this.panneau()?.type === 'creation');
  readonly compteCreeRecemment = computed(() => {
    const panneau = this.panneau();
    return panneau?.type === 'compte-cree' ? panneau.resultat : null;
  });

  ouvrirFiche(compte: CompteAdministration): void {
    this.panneau.set({ type: 'fiche', compte });
  }

  ouvrirCreation(): void {
    this.panneau.set({ type: 'creation' });
  }

  fermerPanneau(): void {
    this.panneau.set(null);
  }

  /** Après une modification dans la fiche : la fiche affiche le compte à jour et la liste se recharge. */
  compteChange(compte: CompteAdministration): void {
    this.panneau.set({ type: 'fiche', compte });
    this.rechargement.update((valeur) => valeur + 1);
  }

  compteCree(resultat: CompteEtMotDePasse): void {
    this.panneau.set({ type: 'compte-cree', resultat });
    this.rechargement.update((valeur) => valeur + 1);
  }
}
