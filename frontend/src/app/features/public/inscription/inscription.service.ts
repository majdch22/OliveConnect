import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Une délégation proposée dans la liste (miroir de DelegationReponse en Java). */
export interface Delegation {
  code: string;
  libelle: string;
}

/** Formulaire envoyé au serveur (miroir de InscriptionRequete en Java). */
export interface InscriptionRequete {
  nom: string;
  prenom: string;
  cin: string;
  telephone: string;
  email: string | null;
  delegation: string;
  motDePasse: string;
  confirmationMotDePasse: string;
  conditionsAcceptees: boolean;
}

/** Réponse à une inscription enregistrée (miroir de InscriptionReponse en Java). */
export interface InscriptionReponse {
  prenom: string;
  telephoneMasque: string;
}

/** Appels au serveur pour l'inscription d'un agriculteur. */
@Injectable({ providedIn: 'root' })
export class InscriptionService {
  private readonly http = inject(HttpClient);

  delegations(): Observable<Delegation[]> {
    return this.http.get<Delegation[]>('/api/inscription/delegations');
  }

  inscrire(requete: InscriptionRequete): Observable<InscriptionReponse> {
    return this.http.post<InscriptionReponse>('/api/inscription', requete);
  }
}
