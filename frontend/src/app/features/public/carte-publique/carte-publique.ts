import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { afterNextRender, Component, computed, effect, ElementRef, inject, OnDestroy, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';

import { echapperHtml } from '../../../shared/carte/echapper-html';
import { Logo } from '../../../shared/logo/logo';

/** Un verger tel que le voit un visiteur (miroir de CartePubliqueController.VergerPublic en Java). */
interface VergerPublic {
  nom: string;
  libelleDelegation: string;
  nomVariete: string;
  superficieHa: number;
  niveauMaturite: number | null;
  statutRecolte: 'NON_MUR' | 'MUR' | 'EN_RECOLTE' | 'RECOLTE';
  libelleStatutRecolte: string;
  alerteEnCours: boolean;
  latitude: number;
  longitude: number;
}

/** Couleur de chaque état sur la carte et dans la légende ; une alerte en cours passe avant l'état. */
const COULEURS = { RECOLTE: '#2f3e12', MUR: '#5b7a1e', EN_RECOLTE: '#5b7a1e', ALERTE: '#b45309', NON_MUR: '#9ca3af' } as const;

const CENTRE_SFAX: L.LatLngExpression = [34.82, 10.6];
const SEUIL_MATURITE = 70;

/**
 * Page publique /carte (visiteurs) : les vergers des adhérents, filtrables par variété, maturité et recherche.
 * Le serveur ne donne ni le nom de l'agriculteur ni la position exacte (arrondie à environ 100 m).
 */
@Component({
  selector: 'app-carte-publique',
  imports: [RouterLink, Logo, DecimalPipe],
  templateUrl: './carte-publique.html',
})
export class CartePublique implements OnDestroy {
  private readonly conteneur = viewChild.required<ElementRef<HTMLDivElement>>('conteneurCarte');
  private carte?: L.Map;
  private readonly couche = L.layerGroup();

  readonly vergers = toSignal(inject(HttpClient).get<VergerPublic[]>('/api/public/vergers'), { initialValue: [] });
  readonly varietes = computed(() => [...new Set(this.vergers().map((verger) => verger.nomVariete))].sort((a, b) => a.localeCompare(b, 'fr')));

  readonly recherche = signal('');
  readonly variete = signal<string | null>(null);
  readonly mursSeulement = signal(false);
  readonly seuil = SEUIL_MATURITE;

  readonly vergersAffiches = computed(() => {
    const texte = this.recherche().trim().toLocaleLowerCase('fr');
    return this.vergers().filter(
      (verger) =>
        (!texte || `${verger.nom} ${verger.libelleDelegation}`.toLocaleLowerCase('fr').includes(texte)) &&
        (!this.variete() || verger.nomVariete === this.variete()) &&
        (!this.mursSeulement() || (verger.niveauMaturite ?? 0) >= SEUIL_MATURITE),
    );
  });

  readonly legende = [
    { libelle: 'Récolté', couleur: COULEURS.RECOLTE },
    { libelle: 'Mûr ou en récolte', couleur: COULEURS.MUR },
    { libelle: 'Alerte en cours', couleur: COULEURS.ALERTE },
    { libelle: 'Pas encore mûr', couleur: COULEURS.NON_MUR },
  ];

  constructor() {
    afterNextRender(() => {
      this.carte = L.map(this.conteneur().nativeElement).setView(CENTRE_SFAX, 9);
      L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; contributeurs OpenStreetMap, fond OpenStreetMap France',
      }).addTo(this.carte);
      this.couche.addTo(this.carte);
      this.dessiner(this.vergersAffiches());
    });
    // Redessine à chaque changement de filtre
    effect(() => this.dessiner(this.vergersAffiches()));
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  couleur(verger: VergerPublic): string {
    return verger.alerteEnCours ? COULEURS.ALERTE : COULEURS[verger.statutRecolte];
  }

  centrer(verger: VergerPublic): void {
    this.carte?.flyTo([verger.latitude, verger.longitude], 13);
  }

  private dessiner(vergers: VergerPublic[]): void {
    if (!this.carte) {
      return;
    }
    this.couche.clearLayers();
    for (const verger of vergers) {
      const maturite = verger.niveauMaturite === null ? 'pas encore mesurée' : `${verger.niveauMaturite} %`;
      L.circleMarker([verger.latitude, verger.longitude], {
        radius: 8, color: '#ffffff', weight: 2, fillColor: this.couleur(verger), fillOpacity: 0.95,
      })
        .bindPopup(`<strong>${echapperHtml(verger.nom)}</strong> — ${echapperHtml(verger.libelleDelegation)}<br>
          ${echapperHtml(verger.libelleStatutRecolte)}${verger.alerteEnCours ? ' · alerte en cours' : ''}<br>
          ${echapperHtml(verger.nomVariete)} · ${verger.superficieHa.toLocaleString('fr-FR')} ha · maturité ${maturite}`)
        .addTo(this.couche);
    }
  }
}
