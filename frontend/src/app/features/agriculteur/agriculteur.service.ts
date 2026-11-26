import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Demande,
  DemandeRequete,
  Profil,
  Signalement,
  SignalementRequete,
  Verger,
  VergerRequete,
} from './agriculteur.modeles';

const ADRESSE = '/api/agriculteur';

/**
 * Appels au serveur de l'espace agriculteur.
 * Le serveur identifie toujours l'agriculteur par son jeton d'accès : aucune de ces requêtes ne transmet son identifiant.
 */
@Injectable({ providedIn: 'root' })
export class AgriculteurService {
  private readonly http = inject(HttpClient);

  // --- Vergers ---

  vergers(): Observable<Verger[]> {
    return this.http.get<Verger[]>(`${ADRESSE}/vergers`);
  }

  enregistrerVerger(id: string | null, requete: VergerRequete): Observable<Verger> {
    return id ? this.http.put<Verger>(`${ADRESSE}/vergers/${id}`, requete) : this.http.post<Verger>(`${ADRESSE}/vergers`, requete);
  }

  changerArchivageVerger(id: string, archiver: boolean): Observable<Verger> {
    return this.http.post<Verger>(`${ADRESSE}/vergers/${id}/${archiver ? 'archiver' : 'restaurer'}`, null);
  }

  estimation(vergerId: string): Observable<{ quantiteKg: number }> {
    return this.http.get<{ quantiteKg: number }>(`${ADRESSE}/vergers/${vergerId}/estimation`);
  }

  // --- Demandes de récolte ---

  demandes(): Observable<Demande[]> {
    return this.http.get<Demande[]>(`${ADRESSE}/demandes`);
  }

  enregistrerDemande(id: string | null, requete: DemandeRequete): Observable<Demande> {
    return id ? this.http.put<Demande>(`${ADRESSE}/demandes/${id}`, requete) : this.http.post<Demande>(`${ADRESSE}/demandes`, requete);
  }

  annulerDemande(id: string, motif: string | null): Observable<Demande> {
    return this.http.post<Demande>(`${ADRESSE}/demandes/${id}/annuler`, { motif });
  }

  // --- Signalements ---

  signalements(): Observable<Signalement[]> {
    return this.http.get<Signalement[]>(`${ADRESSE}/signalements`);
  }

  signaler(requete: SignalementRequete): Observable<Signalement> {
    return this.http.post<Signalement>(`${ADRESSE}/signalements`, requete);
  }

  // --- Listes et profil (communs à tous les rôles) ---

  varietes(): Observable<{ id: string; nom: string }[]> {
    return this.http.get<{ id: string; nom: string }[]>('/api/listes/varietes');
  }

  delegations(): Observable<{ code: string; libelle: string }[]> {
    return this.http.get<{ code: string; libelle: string }[]>('/api/inscription/delegations');
  }

  profil(): Observable<Profil> {
    return this.http.get<Profil>('/api/profil');
  }
}
