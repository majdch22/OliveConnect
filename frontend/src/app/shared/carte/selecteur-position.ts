import { afterNextRender, Component, ElementRef, input, OnDestroy, output, signal, viewChild } from '@angular/core';
import * as L from 'leaflet';

/** Une position GPS en degrés décimaux. */
export interface Position {
  latitude: number;
  longitude: number;
}

const CENTRE_SFAX: L.LatLngExpression = [34.82, 10.6];

/**
 * Carte pour choisir une position : un toucher (ou clic) place le repère, le bouton « Ma position »
 * utilise le GPS du téléphone. Si le GPS est refusé, l'utilisateur place le repère à la main.
 */
@Component({
  selector: 'app-selecteur-position',
  template: `
    <div class="overflow-hidden rounded-xl border border-gray-300">
      <div #conteneurCarte class="h-64 w-full sm:h-72" role="application" aria-label="Carte : touchez pour placer la position"></div>
    </div>
    <div class="mt-2 flex flex-wrap items-center gap-3">
      <button type="button" class="bouton-secondaire px-4 py-2 text-sm" (click)="utiliserLeGps()" [disabled]="recherche()">
        📍 {{ recherche() ? 'Recherche de la position…' : 'Ma position' }}
      </button>
      <span class="text-sm text-gray-600">
        @if (position(); as point) {
          {{ point.latitude.toFixed(5) }}, {{ point.longitude.toFixed(5) }}
          @if (precisionMetres(); as precision) { · précision {{ precision }} m }
        } @else {
          Touchez la carte pour placer la position.
        }
      </span>
    </div>
    @if (messageGps(); as message) {
      <p class="mt-2 text-sm text-amber-800">{{ message }}</p>
    }
  `,
})
export class SelecteurPosition implements OnDestroy {
  private readonly conteneur = viewChild.required<ElementRef<HTMLDivElement>>('conteneurCarte');
  private carte?: L.Map;
  private repere?: L.CircleMarker;

  /** Position de départ (modification d'un élément existant). */
  readonly positionInitiale = input<Position | null>(null);
  /** Repère secondaire affiché pour situer, par exemple le verger lors d'un signalement. */
  readonly reference = input<Position | null>(null);
  readonly positionChoisie = output<Position>();

  readonly position = signal<Position | null>(null);
  readonly precisionMetres = signal<number | null>(null);
  readonly recherche = signal(false);
  readonly messageGps = signal<string | null>(null);

  constructor() {
    afterNextRender(() => {
      const depart = this.positionInitiale() ?? this.reference();
      this.carte = L.map(this.conteneur().nativeElement).setView(
        depart ? [depart.latitude, depart.longitude] : CENTRE_SFAX,
        depart ? 14 : 9,
      );
      L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; contributeurs OpenStreetMap',
      }).addTo(this.carte);
      const reference = this.reference();
      if (reference) {
        L.circleMarker([reference.latitude, reference.longitude], { radius: 6, color: '#9ca3af', fillOpacity: 0.6 })
          .bindTooltip('Votre verger')
          .addTo(this.carte);
      }
      if (this.positionInitiale()) {
        this.placer(this.positionInitiale()!, false);
      }
      this.carte.on('click', (evenement: L.LeafletMouseEvent) => {
        this.precisionMetres.set(null);
        this.placer({ latitude: evenement.latlng.lat, longitude: evenement.latlng.lng }, false);
      });
    });
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  /** Demande la position au téléphone ; en cas de refus, on explique comment faire à la main. */
  utiliserLeGps(): void {
    if (!navigator.geolocation) {
      this.messageGps.set("Ce navigateur ne donne pas accès au GPS : touchez la carte pour placer la position.");
      return;
    }
    this.recherche.set(true);
    this.messageGps.set(null);
    navigator.geolocation.getCurrentPosition(
      (resultat) => {
        this.recherche.set(false);
        this.precisionMetres.set(Math.round(resultat.coords.accuracy));
        this.placer({ latitude: resultat.coords.latitude, longitude: resultat.coords.longitude }, true);
      },
      () => {
        this.recherche.set(false);
        this.messageGps.set("Position GPS indisponible ou refusée : touchez la carte pour placer la position.");
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  }

  private placer(position: Position, centrer: boolean): void {
    this.position.set(position);
    const point: L.LatLngExpression = [position.latitude, position.longitude];
    if (this.repere) {
      this.repere.setLatLng(point);
    } else if (this.carte) {
      this.repere = L.circleMarker(point, { radius: 9, color: '#ffffff', weight: 3, fillColor: '#5b7a1e', fillOpacity: 1 }).addTo(this.carte);
    }
    if (centrer) {
      this.carte?.setView(point, 16);
    }
    this.positionChoisie.emit(position);
  }
}
