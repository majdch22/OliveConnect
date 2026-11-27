import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CapaciteDuJour, MonPressoir, Page, Reception, ReceptionRequete, TableauDeBordPressoir, TourneeAttendue } from './pressoir.modeles';

const ADRESSE = '/api/pressoir';

/** Appels au serveur de l'espace du responsable du pressoir. Le serveur sait à quel pressoir le compte est rattaché. */
@Injectable({ providedIn: 'root' })
export class PressoirService {
  private readonly http = inject(HttpClient);

  monPressoir(): Observable<MonPressoir> {
    return this.http.get<MonPressoir>(ADRESSE);
  }

  livraisons(date: string): Observable<TourneeAttendue[]> {
    return this.http.get<TourneeAttendue[]>(`${ADRESSE}/livraisons`, { params: { date } });
  }

  receptionner(requete: ReceptionRequete): Observable<Reception> {
    return this.http.post<Reception>(`${ADRESSE}/receptions`, requete);
  }

  receptions(du: string, au: string, page: number, taille = 20): Observable<Page<Reception>> {
    return this.http.get<Page<Reception>>(`${ADRESSE}/receptions`, { params: new HttpParams({ fromObject: { du, au, page, taille } }) });
  }

  /** Le fichier est protégé par le jeton : on le télécharge, puis le navigateur l'enregistre. */
  exporter(du: string, au: string): Observable<Blob> {
    return this.http.get(`${ADRESSE}/receptions/export`, { params: { du, au }, responseType: 'blob' });
  }

  capacites(du: string, au: string): Observable<CapaciteDuJour[]> {
    return this.http.get<CapaciteDuJour[]>(`${ADRESSE}/capacites`, { params: { du, au } });
  }

  publierCapacite(date: string, maximumKg: number, motif: string | null): Observable<CapaciteDuJour> {
    return this.http.put<CapaciteDuJour>(`${ADRESSE}/capacites/${date}`, { maximumKg, motif });
  }

  tableauDeBord(jours: number): Observable<TableauDeBordPressoir> {
    return this.http.get<TableauDeBordPressoir>(`${ADRESSE}/tableau-de-bord`, { params: { jours } });
  }
}
