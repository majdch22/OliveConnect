import { afterNextRender, Component, effect, ElementRef, input, OnDestroy, output, viewChild } from '@angular/core';
import * as L from 'leaflet';

import { echapperHtml } from '../../../shared/carte/echapper-html';
import { StatutRecolte, Verger } from '../agriculteur.modeles';

/** Couleur du point de chaque état de récolte (les mêmes que les pastilles de la liste). */
const COULEURS: Record<StatutRecolte, string> = { NON_MUR: '#9ca3af', MUR: '#5b7a1e', EN_RECOLTE: '#d97706', RECOLTE: '#2f3e12' };

/** Vue « Carte » de « Mes vergers » : un point par verger, cadré sur l'ensemble ; toucher un point ouvre le verger. */
@Component({
  selector: 'app-carte-vergers',
  template: `<div #conteneur class="h-[28rem] w-full rounded-2xl border border-gray-200" role="region" aria-label="Carte de mes vergers"></div>`,
})
export class CarteVergers implements OnDestroy {
  readonly vergers = input.required<Verger[]>();
  readonly choisi = output<Verger>();

  private readonly conteneur = viewChild.required<ElementRef<HTMLDivElement>>('conteneur');
  private carte?: L.Map;
  private readonly couche = L.layerGroup();

  constructor() {
    afterNextRender(() => {
      this.carte = L.map(this.conteneur().nativeElement).setView([34.82, 10.6], 9);
      L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; contributeurs OpenStreetMap, fond OpenStreetMap France',
      }).addTo(this.carte);
      this.couche.addTo(this.carte);
      this.dessiner(this.vergers());
    });
    effect(() => this.dessiner(this.vergers()));
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  private dessiner(vergers: Verger[]): void {
    if (!this.carte) {
      return;
    }
    this.couche.clearLayers();
    for (const verger of vergers) {
      L.circleMarker([verger.latitude, verger.longitude], {
        radius: 10, color: '#ffffff', weight: 2, fillColor: COULEURS[verger.statutRecolte], fillOpacity: verger.archive ? 0.4 : 0.95,
      })
        .bindTooltip(`<strong>${echapperHtml(verger.nom)}</strong><br>${echapperHtml(verger.libelleStatutRecolte)}`, { direction: 'top' })
        .on('click', () => this.choisi.emit(verger))
        .addTo(this.couche);
    }
    if (vergers.length > 0) {
      this.carte.fitBounds(L.latLngBounds(vergers.map((verger) => [verger.latitude, verger.longitude] as L.LatLngTuple)), {
        padding: [40, 40],
        maxZoom: 14,
      });
    }
  }
}
