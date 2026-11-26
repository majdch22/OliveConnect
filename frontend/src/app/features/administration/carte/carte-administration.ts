import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, Component, computed, effect, ElementRef, inject, OnDestroy, signal, viewChild } from '@angular/core';
import * as L from 'leaflet';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { DelegationSurCarte, DonneesCarte, Pressoir } from '../administration.modeles';
import { AdministrationService } from '../administration.service';

/** Centre de la région de Sfax et niveau de zoom de départ. */
const CENTRE_SFAX: L.LatLngExpression = [34.82, 10.6];
const ZOOM_DEPART = 9;
const OLIVE = '#5b7a1e';

/**
 * Page /administration/carte : carte interactive de la région de Sfax (Leaflet + OpenStreetMap).
 * - Pressoirs : un repère par pressoir, avec sa capacité et ses responsables.
 * - Agriculteurs : un cercle par délégation, d'autant plus grand qu'elle compte d'agriculteurs actifs.
 * Chaque couche s'affiche ou se masque ; les listes à droite donnent les mêmes chiffres sans la carte.
 */
@Component({
  selector: 'app-carte-administration',
  imports: [DecimalPipe],
  templateUrl: './carte-administration.html',
})
export class CarteAdministration implements OnDestroy {
  private readonly administration = inject(AdministrationService);
  private readonly conteneur = viewChild.required<ElementRef<HTMLDivElement>>('conteneurCarte');

  private carte?: L.Map;
  private readonly couchePressoirs = L.layerGroup();
  private readonly coucheAgriculteurs = L.layerGroup();
  private readonly reperesPressoirs = new Map<string, L.Marker>();

  readonly donnees = signal<DonneesCarte | null>(null);
  readonly messageErreur = signal<string | null>(null);
  readonly afficherPressoirs = signal(true);
  readonly afficherAgriculteurs = signal(true);

  /** Délégations classées de la plus peuplée à la moins peuplée. */
  readonly delegationsClassees = computed(() =>
    [...(this.donnees()?.delegations ?? [])].sort(
      (a, b) => b.agriculteursActifs - a.agriculteursActifs || a.libelle.localeCompare(b.libelle, 'fr'),
    ),
  );
  readonly totalAgriculteurs = computed(() =>
    (this.donnees()?.delegations ?? []).reduce((total, delegation) => total + delegation.agriculteursActifs, 0),
  );

  /** Exemples de tailles de cercle affichés dans la légende. */
  readonly exemplesLegende = [1, 5, 10];

  constructor() {
    // La carte se crée une fois la page affichée (Leaflet a besoin de l'élément réel)
    afterNextRender(() => {
      this.carte = L.map(this.conteneur().nativeElement, { zoomControl: true }).setView(CENTRE_SFAX, ZOOM_DEPART);
      // Fond OpenStreetMap France : noms de lieux en français quand ils existent
      L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; contributeurs OpenStreetMap, fond OpenStreetMap France',
      }).addTo(this.carte);
      this.couchePressoirs.addTo(this.carte);
      this.coucheAgriculteurs.addTo(this.carte);
      this.charger();
    });

    // Afficher ou masquer chaque couche selon les cases cochées
    effect(() => this.basculerCouche(this.couchePressoirs, this.afficherPressoirs()));
    effect(() => this.basculerCouche(this.coucheAgriculteurs, this.afficherAgriculteurs()));
  }

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  /** Rayon du cercle d'une délégation : sa surface est proportionnelle au nombre d'agriculteurs. */
  rayon(nombre: number): number {
    return nombre === 0 ? 5 : 8 + 6 * Math.sqrt(nombre);
  }

  centrerSurPressoir(pressoir: Pressoir): void {
    this.afficherPressoirs.set(true);
    this.carte?.flyTo([pressoir.latitude, pressoir.longitude], 12);
    this.reperesPressoirs.get(pressoir.id)?.openPopup();
  }

  centrerSurDelegation(delegation: DelegationSurCarte): void {
    this.afficherAgriculteurs.set(true);
    this.carte?.flyTo([delegation.latitude, delegation.longitude], 11);
  }

  private charger(): void {
    this.administration.donneesCarte().subscribe({
      next: (donnees) => {
        this.donnees.set(donnees);
        this.dessinerAgriculteurs(donnees.delegations);
        this.dessinerPressoirs(donnees.pressoirs);
      },
      error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
    });
  }

  /** Cercles proportionnels : olive pour les délégations qui ont des agriculteurs, gris sinon. */
  private dessinerAgriculteurs(delegations: DelegationSurCarte[]): void {
    this.coucheAgriculteurs.clearLayers();
    for (const delegation of delegations) {
      const peuplee = delegation.agriculteursActifs > 0;
      L.circleMarker([delegation.latitude, delegation.longitude], {
        radius: this.rayon(delegation.agriculteursActifs),
        color: peuplee ? OLIVE : '#9ca3af',
        weight: 2,
        fillColor: peuplee ? OLIVE : '#d1d5db',
        fillOpacity: peuplee ? 0.25 : 0.4,
      })
        .bindTooltip(this.texteDelegation(delegation), { direction: 'top' })
        .addTo(this.coucheAgriculteurs);
    }
  }

  /** Un repère en forme de goutte par pressoir, avec une fiche au clic. */
  private dessinerPressoirs(pressoirs: Pressoir[]): void {
    this.couchePressoirs.clearLayers();
    this.reperesPressoirs.clear();
    const icone = L.divIcon({
      className: '',
      iconSize: [28, 36],
      iconAnchor: [14, 34],
      popupAnchor: [0, -30],
      html: `<svg viewBox="0 0 28 36" width="28" height="36" aria-hidden="true">
        <path d="M14 1C7 1 1.5 6.4 1.5 13.3 1.5 22.5 14 35 14 35s12.5-12.5 12.5-21.7C26.5 6.4 21 1 14 1z" fill="#2f3e12" stroke="#fff" stroke-width="2"/>
        <rect x="8.5" y="8" width="11" height="10" rx="1.5" fill="#b9d47a"/></svg>`,
    });
    for (const pressoir of pressoirs) {
      const repere = L.marker([pressoir.latitude, pressoir.longitude], { icon: icone, title: pressoir.nom })
        .bindPopup(this.fichePressoir(pressoir))
        .addTo(this.couchePressoirs);
      this.reperesPressoirs.set(pressoir.id, repere);
    }
  }

  private texteDelegation(delegation: DelegationSurCarte): string {
    const enAttente = delegation.inscriptionsEnAttente > 0 ? ` · ${delegation.inscriptionsEnAttente} inscription(s) en attente` : '';
    return `<strong>${echapper(delegation.libelle)}</strong><br>${delegation.agriculteursActifs} agriculteur(s) actif(s)${enAttente}`;
  }

  private fichePressoir(pressoir: Pressoir): string {
    return `<strong>${echapper(pressoir.nom)}</strong> <span style="color:#6b7280">${echapper(pressoir.code)}</span><br>
      ${echapper(pressoir.libelleDelegation)}<br>
      Capacité : ${pressoir.capaciteJournaliereKg.toLocaleString('fr-FR')} kg par jour<br>
      Responsable(s) rattaché(s) : ${pressoir.nombreResponsables}`;
  }

  private basculerCouche(couche: L.LayerGroup, visible: boolean): void {
    if (!this.carte) {
      return;
    }
    if (visible) {
      couche.addTo(this.carte);
    } else {
      couche.remove();
    }
  }
}

/** Les noms viennent de la base : on les échappe avant de les insérer dans les bulles de la carte. */
function echapper(texte: string): string {
  return texte.replace(/[&<>"']/g, (caractere) => `&#${caractere.charCodeAt(0)};`);
}
