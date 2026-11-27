import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AlerteCollecte,
  CompteursAlertes,
  CompteursDemandes,
  DemandeCollecte,
  DonneesCarteCollecte,
  FicheDemande,
  OptionsPlanification,
  Page,
  PlanificationRequete,
  StatutAlerte,
  StatutDemande,
  TableauDeBordCollecte,
  Tournee,
  TypeAlerte,
  TypeRecolte,
  Verification,
} from './collecte.modeles';

const ADRESSE = '/api/collecte';

/** Appels au serveur de l'espace du responsable de collecte. */
@Injectable({ providedIn: 'root' })
export class CollecteService {
  private readonly http = inject(HttpClient);

  /** Signalements ouverts : pastille du menu « Alertes », relue après chaque action. */
  readonly alertesOuvertes = signal(0);

  // --- Tableau de bord ---

  tableauDeBord(): Observable<TableauDeBordCollecte> {
    return this.http.get<TableauDeBordCollecte>(`${ADRESSE}/tableau-de-bord`);
  }

  // --- Demandes ---

  demandes(filtres: { statut: StatutDemande | null; type: TypeRecolte | null; texte: string; page: number; taille: number }): Observable<Page<DemandeCollecte>> {
    let parametres = new HttpParams().set('page', filtres.page).set('taille', filtres.taille);
    if (filtres.statut) parametres = parametres.set('statut', filtres.statut);
    if (filtres.type) parametres = parametres.set('type', filtres.type);
    if (filtres.texte.trim()) parametres = parametres.set('texte', filtres.texte.trim());
    return this.http.get<Page<DemandeCollecte>>(`${ADRESSE}/demandes`, { params: parametres });
  }

  compteursDemandes(): Observable<CompteursDemandes> {
    return this.http.get<CompteursDemandes>(`${ADRESSE}/demandes/compteurs`);
  }

  ficheDemande(id: string): Observable<FicheDemande> {
    return this.http.get<FicheDemande>(`${ADRESSE}/demandes/${id}`);
  }

  validerDemande(id: string, commentaire: string | null): Observable<DemandeCollecte> {
    return this.http.post<DemandeCollecte>(`${ADRESSE}/demandes/${id}/valider`, { commentaire });
  }

  rejeterDemande(id: string, motif: string): Observable<DemandeCollecte> {
    return this.http.post<DemandeCollecte>(`${ADRESSE}/demandes/${id}/rejeter`, { motif });
  }

  // --- Planification et tournées ---

  optionsPlanification(date: string): Observable<OptionsPlanification> {
    return this.http.get<OptionsPlanification>(`${ADRESSE}/planification/options`, { params: { date } });
  }

  verifier(requete: PlanificationRequete): Observable<Verification> {
    return this.http.post<Verification>(`${ADRESSE}/planification/verification`, requete);
  }

  creerTournee(requete: PlanificationRequete): Observable<Tournee> {
    return this.http.post<Tournee>(`${ADRESSE}/tournees`, requete);
  }

  tournees(date: string): Observable<Tournee[]> {
    return this.http.get<Tournee[]>(`${ADRESSE}/tournees`, { params: { date } });
  }

  annulerTournee(id: string, motif: string): Observable<Tournee> {
    return this.http.post<Tournee>(`${ADRESSE}/tournees/${id}/annuler`, { motif });
  }

  // --- Signalements ---

  alertes(statut: StatutAlerte | null, type: TypeAlerte | null, page: number): Observable<Page<AlerteCollecte>> {
    let parametres = new HttpParams().set('page', page).set('taille', 20);
    if (statut) parametres = parametres.set('statut', statut);
    if (type) parametres = parametres.set('type', type);
    return this.http.get<Page<AlerteCollecte>>(`${ADRESSE}/alertes`, { params: parametres });
  }

  compteursAlertes(): Observable<CompteursAlertes> {
    return this.http.get<CompteursAlertes>(`${ADRESSE}/alertes/compteurs`).pipe(tap((compteurs) => this.alertesOuvertes.set(compteurs.OUVERTE)));
  }

  /** Relit le nombre de signalements ouverts (pastille du menu). */
  rafraichirAlertesOuvertes(): void {
    this.compteursAlertes().subscribe({ error: () => undefined });
  }

  prendreEnCharge(id: string, commentaire: string | null): Observable<AlerteCollecte> {
    return this.http.post<AlerteCollecte>(`${ADRESSE}/alertes/${id}/prendre-en-charge`, { commentaire });
  }

  resoudre(id: string, commentaire: string): Observable<AlerteCollecte> {
    return this.http.post<AlerteCollecte>(`${ADRESSE}/alertes/${id}/resoudre`, { commentaire });
  }

  creerDemandeUrgente(id: string, date: string, quantiteEstimeeKg: number, commentaire: string | null): Observable<AlerteCollecte> {
    return this.http.post<AlerteCollecte>(`${ADRESSE}/alertes/${id}/demande-urgente`, { date, quantiteEstimeeKg, commentaire });
  }

  /** La photo est protégée par le jeton : on la télécharge, puis le navigateur l'affiche depuis la mémoire. */
  photo(alerteId: string): Observable<Blob> {
    return this.http.get(`${ADRESSE}/alertes/${alerteId}/photo`, { responseType: 'blob' });
  }

  // --- Carte ---

  carte(zone: { ouest: number; sud: number; est: number; nord: number } | null): Observable<DonneesCarteCollecte> {
    const parametres = zone ? new HttpParams({ fromObject: { ...zone } }) : new HttpParams();
    return this.http.get<DonneesCarteCollecte>(`${ADRESSE}/carte`, { params: parametres });
  }
}
