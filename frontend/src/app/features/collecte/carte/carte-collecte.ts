import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, Component, computed, effect, ElementRef, inject, OnDestroy, signal, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import * as L from 'leaflet';
import { Subject, debounceTime, switchMap } from 'rxjs';

import { texteErreur } from '../../../core/erreurs/message-erreur';
import { echapperHtml } from '../../../shared/carte/echapper-html';
import { StatutRecolte } from '../../agriculteur/agriculteur.modeles';
import { AlerteSurCarte, DonneesCarteCollecte, PressoirSurCarte, VergerSurCarte } from '../collecte.modeles';
import { CollecteService } from '../collecte.service';

const CENTRE_SFAX: L.LatLngExpression = [34.82, 10.6];
const ZOOM_DEPART = 9;

/** Couleur et libellé de chaque statut de récolte (légende et repères). */
export const APPARENCE_STATUT_RECOLTE: Record<StatutRecolte, { libelle: string; couleur: string }> = {
  MUR: { libelle: 'Mûr', couleur: '#5b7a1e' },
  NON_MUR: { libelle: 'Non mûr', couleur: '#b9d47a' },
  EN_RECOLTE: { libelle: 'En récolte', couleur: '#0369a1' },
  RECOLTE: { libelle: 'Récolté', couleur: '#6b7280' },
};

/**
 * Page /collecte/carte (maquette RC4) : vergers (couleur = statut de récolte, cercle pointillé = tournée du jour),
 * signalements non résolus et pressoirs.
 * Quand la carte bouge, seuls les vergers de la zone visible sont demandés au serveur (requête $geoWithin, index 2dsphere).
 * Filtres : statut de récolte, variété, maturité minimale ; chaque couche s'affiche ou se masque.
 */
@Component({
  selector: 'app-carte-collecte',
  templateUrl: './carte-collecte.html',
})
export class CarteCollecte implements OnDestroy {
  private readonly service = inject(CollecteService);
  private readonly conteneur = viewChild.required<ElementRef<HTMLDivElement>>('conteneurCarte');
  private readonly alerteDemandee = inject(ActivatedRoute).snapshot.queryParamMap.get('alerte');

  private carte?: L.Map;
  private readonly coucheVergers = L.layerGroup();
  private readonly coucheAlertes = L.layerGroup();
  private readonly couchePressoirs = L.layerGroup();
  private readonly reperesVergers = new Map<string, L.CircleMarker>();
  private readonly reperesAlertes = new Map<string, L.Marker>();
  private readonly zoneVisible = new Subject<{ ouest: number; sud: number; est: number; nord: number }>();

  readonly apparences = APPARENCE_STATUT_RECOLTE;
  readonly statuts = Object.keys(APPARENCE_STATUT_RECOLTE) as StatutRecolte[];
  readonly donnees = signal<DonneesCarteCollecte | null>(null);
  readonly messageErreur = signal<string | null>(null);

  // Filtres et couches
  readonly statutsAffiches = signal<StatutRecolte[]>([...this.statuts]);
  readonly variete = signal('');
  readonly maturiteMinimum = signal(0);
  readonly afficherVergers = signal(true);
  readonly afficherAlertes = signal(true);
  readonly afficherPressoirs = signal(true);
  readonly seulementTourneesDuJour = signal(false);

  readonly varietes = computed(() => [...new Set((this.donnees()?.vergers ?? []).map((verger) => verger.nomVariete))].sort((a, b) => a.localeCompare(b, 'fr')));
  readonly vergersFiltres = computed(() =>
    (this.donnees()?.vergers ?? []).filter(
      (verger) =>
        this.statutsAffiches().includes(verger.statutRecolte) &&
        (!this.variete() || verger.nomVariete === this.variete()) &&
        (verger.niveauMaturite ?? 0) >= this.maturiteMinimum() &&
        (!this.seulementTourneesDuJour() || verger.tourneeDuJour),
    ),
  );

  constructor() {
    this.zoneVisible
      .pipe(debounceTime(300), switchMap((zone) => this.service.carte(zone)))
      .subscribe({
        next: (donnees) => this.donnees.set(donnees),
        error: (erreur: HttpErrorResponse) => this.messageErreur.set(texteErreur(erreur)),
      });

    afterNextRender(() => {
      this.carte = L.map(this.conteneur().nativeElement).setView(CENTRE_SFAX, ZOOM_DEPART);
      L.tileLayer('https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; contributeurs OpenStreetMap, fond OpenStreetMap France',
      }).addTo(this.carte);
      // Les couches visibles sont posées sur la carte maintenant qu'elle existe
      this.basculerCouche(this.coucheVergers, this.afficherVergers());
      this.basculerCouche(this.coucheAlertes, this.afficherAlertes());
      this.basculerCouche(this.couchePressoirs, this.afficherPressoirs());
      this.carte.on('moveend', () => this.demanderZone());
      this.demanderZone();
    });

    effect(() => this.dessinerVergers(this.vergersFiltres()));
    effect(() => this.dessinerAlertes(this.donnees()?.alertes ?? []));
    effect(() => this.dessinerPressoirs(this.donnees()?.pressoirs ?? []));
    effect(() => this.basculerCouche(this.coucheVergers, this.afficherVergers()));
    effect(() => this.basculerCouche(this.coucheAlertes, this.afficherAlertes()));
    effect(() => this.basculerCouche(this.couchePressoirs, this.afficherPressoirs()));
    // Arrivée depuis la fiche d'un signalement : on centre la carte dessus
    effect(() => {
      const alerte = this.donnees()?.alertes.find((candidate) => candidate.id === this.alerteDemandee);
      if (alerte && !this.alerteCentree) {
        this.alerteCentree = true;
        this.carte?.setView([alerte.latitude, alerte.longitude], 14);
        setTimeout(() => this.reperesAlertes.get(alerte.id)?.openPopup(), 400);
      }
    });
  }
  private alerteCentree = false;

  ngOnDestroy(): void {
    this.carte?.remove();
  }

  basculerStatut(statut: StatutRecolte): void {
    this.statutsAffiches.update((affiches) => (affiches.includes(statut) ? affiches.filter((autre) => autre !== statut) : [...affiches, statut]));
  }

  centrerSurVerger(verger: VergerSurCarte): void {
    this.afficherVergers.set(true);
    this.carte?.flyTo([verger.latitude, verger.longitude], 14);
    setTimeout(() => this.reperesVergers.get(verger.id)?.openPopup(), 600);
  }

  /** Zone visible, un peu agrandie pour que les vergers du bord ne disparaissent pas en déplaçant la carte. */
  private demanderZone(): void {
    if (!this.carte) return;
    const limites = this.carte.getBounds().pad(0.2);
    this.zoneVisible.next({
      ouest: Math.max(limites.getWest(), -180), sud: Math.max(limites.getSouth(), -85),
      est: Math.min(limites.getEast(), 180), nord: Math.min(limites.getNorth(), 85),
    });
  }

  private dessinerVergers(vergers: VergerSurCarte[]): void {
    this.coucheVergers.clearLayers();
    this.reperesVergers.clear();
    for (const verger of vergers) {
      const apparence = APPARENCE_STATUT_RECOLTE[verger.statutRecolte];
      if (verger.tourneeDuJour) {
        L.circleMarker([verger.latitude, verger.longitude], { radius: 15, color: '#2f3e12', weight: 2, dashArray: '4 3', fill: false, interactive: false })
          .addTo(this.coucheVergers);
      }
      const repere = L.circleMarker([verger.latitude, verger.longitude], {
        radius: 9, color: '#ffffff', weight: 2, fillColor: apparence.couleur, fillOpacity: 1,
      })
        .bindPopup(this.ficheVerger(verger))
        .bindTooltip(echapperHtml(verger.nom), { direction: 'top', offset: [0, -8] })
        .addTo(this.coucheVergers);
      this.reperesVergers.set(verger.id, repere);
    }
  }

  private dessinerAlertes(alertes: AlerteSurCarte[]): void {
    this.coucheAlertes.clearLayers();
    this.reperesAlertes.clear();
    const icone = L.divIcon({
      className: '', iconSize: [26, 24], iconAnchor: [13, 22], popupAnchor: [0, -20],
      html: `<svg viewBox="0 0 26 24" width="26" height="24" aria-hidden="true"><path d="M13 1 25 23H1z" fill="#b91c1c" stroke="#fff" stroke-width="2" stroke-linejoin="round"/>
        <path d="M13 9v6M13 18.5v.5" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/></svg>`,
    });
    for (const alerte of alertes) {
      const repere = L.marker([alerte.latitude, alerte.longitude], { icon: icone, title: `${alerte.libelleType} · ${alerte.nomVerger}` })
        .bindPopup(`<strong>${echapperHtml(alerte.libelleType)} · ${echapperHtml(alerte.nomVerger)}</strong><br>
          ${echapperHtml(alerte.numero)} · ${echapperHtml(alerte.libelleStatut)}<br>« ${echapperHtml(alerte.description)} »`)
        .addTo(this.coucheAlertes);
      this.reperesAlertes.set(alerte.id, repere);
    }
  }

  private dessinerPressoirs(pressoirs: PressoirSurCarte[]): void {
    this.couchePressoirs.clearLayers();
    const icone = L.divIcon({
      className: '', iconSize: [28, 36], iconAnchor: [14, 34], popupAnchor: [0, -30],
      html: `<svg viewBox="0 0 28 36" width="28" height="36" aria-hidden="true">
        <path d="M14 1C7 1 1.5 6.4 1.5 13.3 1.5 22.5 14 35 14 35s12.5-12.5 12.5-21.7C26.5 6.4 21 1 14 1z" fill="#2f3e12" stroke="#fff" stroke-width="2"/>
        <rect x="8.5" y="8" width="11" height="10" rx="1.5" fill="#b9d47a"/></svg>`,
    });
    for (const pressoir of pressoirs) {
      L.marker([pressoir.latitude, pressoir.longitude], { icon: icone, title: pressoir.nom })
        .bindPopup(`<strong>${echapperHtml(pressoir.nom)}</strong> <span style="color:#6b7280">${echapperHtml(pressoir.code)}</span><br>
          ${echapperHtml(pressoir.libelleDelegation)} · ${pressoir.capaciteJournaliereKg.toLocaleString('fr-FR')} kg par jour`)
        .addTo(this.couchePressoirs);
    }
  }

  private ficheVerger(verger: VergerSurCarte): string {
    const maturite = verger.niveauMaturite !== null ? `Maturité ${verger.niveauMaturite} %` : 'Maturité non mesurée';
    const lignes = [
      `<strong>${echapperHtml(verger.nom)}</strong> · ${echapperHtml(verger.libelleStatutRecolte)}`,
      `${echapperHtml(verger.nomAgriculteur)} · ${echapperHtml(verger.nomVariete)} · ${verger.superficieHa.toLocaleString('fr-FR')} ha`,
      maturite,
      `${verger.demandesEnCours} demande(s) en cours · ${verger.signalementsNonResolus} signalement(s) non résolu(s)`,
    ];
    if (verger.tourneeDuJour) lignes.push(`Tournée du jour : ${echapperHtml(verger.tourneeDuJour)}`);
    return lignes.join('<br>');
  }

  private basculerCouche(couche: L.LayerGroup, visible: boolean): void {
    if (!this.carte) return;
    if (visible) couche.addTo(this.carte);
    else couche.remove();
  }
}
