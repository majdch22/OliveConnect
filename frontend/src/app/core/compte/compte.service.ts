import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Comment la personne veut être prévenue (miroir de PreferencesNotification en Java). */
export interface PreferencesNotification {
  application: boolean;
  sms: boolean;
}

/** Compte de la personne connectée (miroir de ProfilReponse en Java). */
export interface Profil {
  id: string;
  prenom: string;
  nom: string;
  email: string | null;
  telephone: string;
  role: string;
  libelleRole: string;
  motDePasseProvisoire: boolean;
  numeroAdherent: string | null;
  delegation: string | null;
  libelleDelegation: string | null;
  preferences: PreferencesNotification;
  visiteGuideeTerminee: boolean;
}

export interface CoordonneesRequete {
  telephone: string;
  email: string | null;
  delegation: string | null;
  motDePasse: string;
}

/** « Mon profil » pour tous les rôles : le serveur reconnaît la personne à son jeton d'accès. */
@Injectable({ providedIn: 'root' })
export class CompteService {
  private readonly http = inject(HttpClient);

  profil(): Observable<Profil> {
    return this.http.get<Profil>('/api/profil');
  }

  changerCoordonnees(requete: CoordonneesRequete): Observable<Profil> {
    return this.http.put<Profil>('/api/profil/coordonnees', requete);
  }

  changerPreferences(preferences: PreferencesNotification): Observable<Profil> {
    return this.http.put<Profil>('/api/profil/preferences', preferences);
  }

  /** La visite guidée est terminée ou passée : elle n'est plus proposée. */
  terminerVisiteGuidee(): Observable<Profil> {
    return this.http.post<Profil>('/api/profil/visite-guidee', null);
  }

  delegations(): Observable<{ code: string; libelle: string }[]> {
    return this.http.get<{ code: string; libelle: string }[]>('/api/inscription/delegations');
  }
}
