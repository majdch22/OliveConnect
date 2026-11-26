import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  CategorieQualite,
  CompteAdministration,
  CompteEtMotDePasse,
  CompteRequete,
  DonneesCarte,
  Equipe,
  EtatServeur,
  FiltresComptes,
  FiltresInscriptions,
  FiltresReferentiel,
  FiltresRessources,
  OptionListe,
  PageResultat,
  Parametres,
  ParametresReponse,
  Pressoir,
  PressoirRequete,
  Ressource,
  RessourceRequete,
  ResumeRessources,
  Sauvegarde,
  StatistiquesComptes,
  Variete,
  VarieteRequete,
} from './administration.modeles';

const ADRESSE = '/api/administration';

/**
 * Appels au serveur de l'espace d'administration.
 * Garde aussi les statistiques en mémoire, pour que le badge du menu se mette à jour après chaque action.
 */
@Injectable({ providedIn: 'root' })
export class AdministrationService {
  private readonly http = inject(HttpClient);

  private readonly statistiquesCourantes = signal<StatistiquesComptes | null>(null);
  readonly statistiques = this.statistiquesCourantes.asReadonly();

  /** Délégations de Sfax, pour le filtre par délégation. */
  delegations(): Observable<OptionListe[]> {
    return this.http.get<OptionListe[]>('/api/inscription/delegations');
  }

  rafraichirStatistiques(): void {
    this.http
      .get<StatistiquesComptes>(`${ADRESSE}/statistiques`)
      .subscribe((statistiques) => this.statistiquesCourantes.set(statistiques));
  }

  // --- Utilisateurs ---

  listerComptes(filtres: FiltresComptes): Observable<PageResultat<CompteAdministration>> {
    return this.http.get<PageResultat<CompteAdministration>>(`${ADRESSE}/utilisateurs`, {
      params: parametresAdresse(filtres),
    });
  }

  creerCompte(requete: CompteRequete): Observable<CompteEtMotDePasse> {
    return this.http
      .post<CompteEtMotDePasse>(`${ADRESSE}/utilisateurs`, requete)
      .pipe(tap(() => this.rafraichirStatistiques()));
  }

  modifierCompte(id: string, requete: CompteRequete): Observable<CompteAdministration> {
    return this.http.put<CompteAdministration>(`${ADRESSE}/utilisateurs/${id}`, requete);
  }

  desactiver(id: string): Observable<CompteAdministration> {
    return this.http
      .post<CompteAdministration>(`${ADRESSE}/utilisateurs/${id}/desactiver`, null)
      .pipe(tap(() => this.rafraichirStatistiques()));
  }

  reactiver(id: string): Observable<CompteAdministration> {
    return this.http
      .post<CompteAdministration>(`${ADRESSE}/utilisateurs/${id}/reactiver`, null)
      .pipe(tap(() => this.rafraichirStatistiques()));
  }

  reinitialiserMotDePasse(id: string): Observable<CompteEtMotDePasse> {
    return this.http.post<CompteEtMotDePasse>(`${ADRESSE}/utilisateurs/${id}/mot-de-passe`, null);
  }

  // --- Supervision ---

  etatServeur(): Observable<EtatServeur> {
    return this.http.get<EtatServeur>(`${ADRESSE}/supervision`);
  }

  sauvegarderMaintenant(): Observable<Sauvegarde> {
    return this.http.post<Sauvegarde>(`${ADRESSE}/sauvegardes`, null);
  }

  // --- Paramètres ---

  parametres(): Observable<ParametresReponse> {
    return this.http.get<ParametresReponse>(`${ADRESSE}/parametres`);
  }

  modifierParametres(parametres: Parametres): Observable<ParametresReponse> {
    return this.http.put<ParametresReponse>(`${ADRESSE}/parametres`, parametres);
  }

  // --- Référentiels ---

  varietes(filtres: FiltresReferentiel): Observable<Variete[]> {
    return this.http.get<Variete[]>(`${ADRESSE}/referentiels/varietes`, { params: parametresAdresse(filtres) });
  }

  enregistrerVariete(id: string | null, requete: VarieteRequete): Observable<Variete> {
    return id
      ? this.http.put<Variete>(`${ADRESSE}/referentiels/varietes/${id}`, requete)
      : this.http.post<Variete>(`${ADRESSE}/referentiels/varietes`, requete);
  }

  changerArchivageVariete(id: string, archiver: boolean): Observable<Variete> {
    return this.http.post<Variete>(`${ADRESSE}/referentiels/varietes/${id}/${archiver ? 'archiver' : 'restaurer'}`, null);
  }

  pressoirs(filtres: FiltresReferentiel): Observable<Pressoir[]> {
    return this.http.get<Pressoir[]>(`${ADRESSE}/referentiels/pressoirs`, { params: parametresAdresse(filtres) });
  }

  enregistrerPressoir(id: string | null, requete: PressoirRequete): Observable<Pressoir> {
    return id
      ? this.http.put<Pressoir>(`${ADRESSE}/referentiels/pressoirs/${id}`, requete)
      : this.http.post<Pressoir>(`${ADRESSE}/referentiels/pressoirs`, requete);
  }

  changerArchivagePressoir(id: string, archiver: boolean): Observable<Pressoir> {
    return this.http.post<Pressoir>(`${ADRESSE}/referentiels/pressoirs/${id}/${archiver ? 'archiver' : 'restaurer'}`, null);
  }

  categoriesQualite(): Observable<CategorieQualite[]> {
    return this.http.get<CategorieQualite[]>(`${ADRESSE}/referentiels/categories-qualite`);
  }

  // --- Ressources et équipes (adresses partagées avec le responsable logistique) ---

  ressources(filtres: FiltresRessources): Observable<PageResultat<Ressource>> {
    return this.http.get<PageResultat<Ressource>>('/api/ressources', { params: parametresAdresse(filtres) });
  }

  resumeRessources(): Observable<ResumeRessources[]> {
    return this.http.get<ResumeRessources[]>('/api/ressources/resume');
  }

  enregistrerRessource(id: string | null, requete: RessourceRequete): Observable<Ressource> {
    return id
      ? this.http.put<Ressource>(`/api/ressources/${id}`, requete)
      : this.http.post<Ressource>('/api/ressources', requete);
  }

  changerArchivageRessource(id: string, archiver: boolean): Observable<Ressource> {
    return this.http.post<Ressource>(`/api/ressources/${id}/${archiver ? 'archiver' : 'restaurer'}`, null);
  }

  equipes(avecArchives = false): Observable<Equipe[]> {
    return this.http.get<Equipe[]>('/api/equipes', { params: { avecArchives } });
  }

  membresEquipe(id: string): Observable<Ressource[]> {
    return this.http.get<Ressource[]>(`/api/equipes/${id}/membres`);
  }

  enregistrerEquipe(id: string | null, nom: string): Observable<Equipe> {
    return id ? this.http.put<Equipe>(`/api/equipes/${id}`, { nom }) : this.http.post<Equipe>('/api/equipes', { nom });
  }

  changerArchivageEquipe(id: string, archiver: boolean): Observable<Equipe> {
    return this.http.post<Equipe>(`/api/equipes/${id}/${archiver ? 'archiver' : 'restaurer'}`, null);
  }

  // --- Carte ---

  donneesCarte(): Observable<DonneesCarte> {
    return this.http.get<DonneesCarte>(`${ADRESSE}/carte`);
  }

  // --- Inscriptions ---

  listerInscriptions(filtres: FiltresInscriptions): Observable<PageResultat<CompteAdministration>> {
    return this.http.get<PageResultat<CompteAdministration>>(`${ADRESSE}/inscriptions`, {
      params: parametresAdresse(filtres),
    });
  }

  accepterInscription(id: string): Observable<CompteAdministration> {
    return this.http
      .post<CompteAdministration>(`${ADRESSE}/inscriptions/${id}/accepter`, null)
      .pipe(tap(() => this.rafraichirStatistiques()));
  }

  refuserInscription(id: string, motif: string): Observable<CompteAdministration> {
    return this.http
      .post<CompteAdministration>(`${ADRESSE}/inscriptions/${id}/refuser`, { motif })
      .pipe(tap(() => this.rafraichirStatistiques()));
  }
}

/** Transforme des filtres en paramètres d'adresse ; les filtres vides ne sont pas envoyés. */
function parametresAdresse(filtres: object): HttpParams {
  let resultat = new HttpParams();
  for (const [nom, valeur] of Object.entries(filtres)) {
    if (valeur !== '' && valeur !== null && valeur !== undefined) {
      resultat = resultat.set(nom, String(valeur).trim());
    }
  }
  return resultat;
}
