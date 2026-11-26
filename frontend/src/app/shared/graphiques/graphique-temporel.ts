import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, ElementRef, inject, input, OnDestroy, OnInit, signal } from '@angular/core';

/** Une mesure à un instant donné ; null quand il n'y a pas de valeur (la courbe s'interrompt). */
export interface PointTemporel {
  moment: string;
  valeur: number | null;
}

const HAUTEUR = 190;
const MARGE_INTERIEURE_CARTE = 42;
const MARGE = { haut: 14, droite: 64, bas: 26, gauche: 44 };

/**
 * Courbe d'une mesure dans le temps (une seule série : le titre la nomme, pas de légende).
 * - Trait de 2 px, aire à 10 % d'opacité, quadrillage discret, valeur actuelle écrite au bout de la courbe.
 * - Survol ou flèches du clavier : une ligne verticale suit le pointeur et une bulle donne la valeur et l'heure.
 * - « Voir les valeurs » : tableau des dernières mesures, pour lire les chiffres sans survol.
 */
@Component({
  selector: 'app-graphique-temporel',
  host: { class: 'block min-w-0' },
  imports: [DatePipe, DecimalPipe],
  templateUrl: './graphique-temporel.html',
})
export class GraphiqueTemporel implements OnInit, OnDestroy {
  private readonly element = inject(ElementRef<HTMLElement>);
  private observateur?: ResizeObserver;

  readonly titre = input.required<string>();
  readonly unite = input.required<string>();
  readonly points = input.required<PointTemporel[]>();
  /** Valeur maximale fixe de l'axe vertical (ex. 100 pour un pourcentage) ; sinon calculée. */
  readonly maximumFixe = input<number | null>(null);

  readonly hauteur = HAUTEUR;
  readonly marge = MARGE;
  readonly largeur = signal(600);
  /** Indice du point survolé (ou choisi au clavier), null si aucun. */
  readonly indiceSurvole = signal<number | null>(null);

  ngOnInit(): void {
    // Sans ResizeObserver (navigateur ancien, environnement de test), le graphique garde sa largeur par défaut
    if (typeof ResizeObserver === 'undefined') {
      return;
    }
    // La largeur suit celle de la carte (moins ses marges intérieures de 2 × 20 px et sa bordure)
    this.observateur = new ResizeObserver(([entree]) =>
      this.largeur.set(Math.max(240, entree.contentRect.width - MARGE_INTERIEURE_CARTE)),
    );
    this.observateur.observe(this.element.nativeElement);
  }

  ngOnDestroy(): void {
    this.observateur?.disconnect();
  }

  readonly valides = computed(() => this.points().filter((point) => point.valeur !== null));
  readonly derniere = computed(() => this.valides().at(-1) ?? null);

  /** Haut de l'axe vertical : arrondi « propre » au-dessus de la plus grande valeur. */
  readonly maximum = computed(() => {
    const fixe = this.maximumFixe();
    if (fixe !== null) {
      return fixe;
    }
    const plusGrande = Math.max(1, ...this.valides().map((point) => point.valeur as number));
    const pas = Math.pow(10, Math.floor(Math.log10(plusGrande)));
    return Math.ceil((plusGrande * 1.15) / pas) * pas;
  });

  readonly graduations = computed(() => [0, 0.25, 0.5, 0.75, 1].map((part) => ({
    valeur: this.maximum() * part,
    y: this.y(this.maximum() * part),
  })));

  /** Trois repères horaires : début, milieu et fin de la période. */
  readonly reperesTemps = computed(() => {
    const points = this.points();
    if (points.length < 2) {
      return [];
    }
    return [0, Math.floor((points.length - 1) / 2), points.length - 1].map((indice) => ({
      moment: points[indice].moment,
      x: this.x(indice),
    }));
  });

  /** Tracé de la courbe, interrompu là où une valeur manque. */
  readonly trace = computed(() => {
    let chemin = '';
    let enCours = false;
    this.points().forEach((point, indice) => {
      if (point.valeur === null) {
        enCours = false;
        return;
      }
      chemin += `${enCours ? 'L' : 'M'}${this.x(indice).toFixed(1)},${this.y(point.valeur).toFixed(1)} `;
      enCours = true;
    });
    return chemin;
  });

  /** Aire sous la courbe : chaque morceau continu est fermé jusqu'à la ligne de base. */
  readonly aire = computed(() => {
    const base = this.y(0);
    let chemin = '';
    let morceau: { x: number; y: number }[] = [];
    const fermer = () => {
      if (morceau.length > 1) {
        chemin += `M${morceau[0].x},${base} ` + morceau.map((p) => `L${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
          + ` L${morceau.at(-1)!.x},${base} Z `;
      }
      morceau = [];
    };
    this.points().forEach((point, indice) => {
      if (point.valeur === null) {
        fermer();
      } else {
        morceau.push({ x: this.x(indice), y: this.y(point.valeur) });
      }
    });
    fermer();
    return chemin;
  });

  readonly pointFinal = computed(() => {
    const points = this.points();
    for (let indice = points.length - 1; indice >= 0; indice--) {
      const valeur = points[indice].valeur;
      if (valeur !== null) {
        return { x: this.x(indice), y: this.y(valeur), valeur };
      }
    }
    return null;
  });

  readonly survol = computed(() => {
    const indice = this.indiceSurvole();
    if (indice === null) {
      return null;
    }
    const point = this.points()[indice];
    return { x: this.x(indice), y: point.valeur === null ? null : this.y(point.valeur), point };
  });

  /** Les 12 dernières mesures, de la plus récente à la plus ancienne, pour le tableau. */
  readonly dernieresMesures = computed(() => [...this.points()].reverse().slice(0, 12));

  /** Le pointeur choisit la mesure la plus proche horizontalement. */
  suivrePointeur(evenement: PointerEvent): void {
    const nombre = this.points().length;
    if (nombre === 0) {
      return;
    }
    const cadre = (evenement.currentTarget as SVGElement).getBoundingClientRect();
    const part = (evenement.clientX - cadre.left - MARGE.gauche) / this.largeurTrace();
    this.indiceSurvole.set(Math.min(nombre - 1, Math.max(0, Math.round(part * (nombre - 1)))));
  }

  /** Flèches gauche / droite : passer d'une mesure à l'autre au clavier. */
  naviguerAuClavier(evenement: KeyboardEvent): void {
    const nombre = this.points().length;
    if (nombre === 0 || (evenement.key !== 'ArrowLeft' && evenement.key !== 'ArrowRight')) {
      return;
    }
    evenement.preventDefault();
    const actuel = this.indiceSurvole() ?? nombre - 1;
    const pas = evenement.key === 'ArrowLeft' ? -1 : 1;
    this.indiceSurvole.set(Math.min(nombre - 1, Math.max(0, actuel + pas)));
  }

  /** Position de la bulle : à droite du repère, ou à gauche s'il est trop près du bord. */
  positionBulle(x: number): string {
    return x > this.largeur() - 160 ? `${x - 150}px` : `${x + 12}px`;
  }

  largeurTrace(): number {
    return this.largeur() - MARGE.gauche - MARGE.droite;
  }

  private x(indice: number): number {
    const nombre = Math.max(1, this.points().length - 1);
    return MARGE.gauche + (indice / nombre) * this.largeurTrace();
  }

  private y(valeur: number): number {
    const hauteurTrace = HAUTEUR - MARGE.haut - MARGE.bas;
    return MARGE.haut + hauteurTrace * (1 - Math.min(valeur, this.maximum()) / this.maximum());
  }
}
