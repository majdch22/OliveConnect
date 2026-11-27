/** Modèles de l'espace du responsable du pressoir (miroirs des classes Java du même nom). */

export type CategorieQualite = 'EXTRA_VIERGE' | 'VIERGE' | 'COURANTE' | 'LAMPANTE';
export type StatutTournee = 'PLANIFIEE' | 'EN_COURS' | 'LIVREE' | 'CLOTUREE' | 'ANNULEE';

export interface CapaciteDuJour {
  date: string;
  capaciteHabituelleKg: number;
  maximumKg: number;
  publiee: boolean;
  motif: string | null;
  nomAuteur: string | null;
  attenduKg: number;
  recuKg: number;
  restantKg: number;
}

export interface MonPressoir {
  pressoir: { id: string; code: string; nom: string; libelleDelegation: string; capaciteJournaliereKg: number; telephone: string | null };
  aujourdhui: CapaciteDuJour;
  demain: CapaciteDuJour;
}

export interface TourneeAttendue {
  id: string;
  numero: string;
  date: string;
  statut: StatutTournee;
  libelleStatut: string;
  nomChefEquipe: string;
  quantiteEstimeeKg: number;
  recuKg: number;
  recevable: boolean;
  bennes: { id: string; code: string; capaciteKg: number; recue: boolean }[];
}

export interface ReceptionRequete {
  tourneeId: string;
  benneId: string;
  poidsNetKg: number;
  aciditePourcent: number;
  commentaire: string | null;
}

export interface Reception {
  id: string;
  numero: string;
  tourneeId: string;
  numeroTournee: string;
  codeBenne: string;
  capaciteBenneKg: number;
  poidsNetKg: number;
  aciditePourcent: number;
  categorie: CategorieQualite;
  libelleCategorie: string;
  tauxRemplissage: number;
  dateHeure: string;
  nomAuteur: string;
  commentaire: string | null;
  parts: { demandeId: string | null; vergerId: string; nomVerger: string; agriculteurId: string; poidsKg: number }[];
  tourneeCloturee: boolean;
}

export interface Page<T> {
  elements: T[];
  page: number;
  taille: number;
  totalElements: number;
  totalPages: number;
}

export interface TableauDeBordPressoir {
  jours: number;
  totaux: { poidsKg: number; bennes: number; remplissageMoyen: number | null; aciditeMoyenne: number | null };
  aujourdhui: CapaciteDuJour;
  parJour: { date: string; poidsKg: number; bennes: number }[];
  parCategorie: { categorie: CategorieQualite; libelle: string; poidsKg: number; bennes: number }[];
  parVariete: { variete: string; poidsKg: number; aciditeMoyenne: number | null }[];
}

/** Catégories de qualité (mêmes seuils que CategorieQualite en Java) : affichées pendant la saisie de l'acidité. */
export const CATEGORIES: { code: CategorieQualite; libelle: string; aciditeMaximale: number; classes: string }[] = [
  { code: 'EXTRA_VIERGE', libelle: 'Extra vierge', aciditeMaximale: 0.8, classes: 'bg-olive-100 text-olive-900' },
  { code: 'VIERGE', libelle: 'Vierge', aciditeMaximale: 2, classes: 'bg-sky-100 text-sky-800' },
  { code: 'COURANTE', libelle: 'Courante', aciditeMaximale: 3.3, classes: 'bg-amber-100 text-amber-800' },
  { code: 'LAMPANTE', libelle: 'Lampante', aciditeMaximale: Number.POSITIVE_INFINITY, classes: 'bg-red-100 text-red-800' },
];

export function categoriePour(acidite: number): (typeof CATEGORIES)[number] {
  return CATEGORIES.find((categorie) => acidite <= categorie.aciditeMaximale) ?? CATEGORIES[3];
}

export function classesCategorie(code: CategorieQualite): string {
  return CATEGORIES.find((categorie) => categorie.code === code)?.classes ?? '';
}

/** Date du jour (avec un décalage en jours) au format AAAA-MM-JJ. */
export function jour(decalage = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + decalage);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

/** « 12,4 t » ou « 850 kg ». */
export function tonnes(kilos: number): string {
  return Math.abs(kilos) >= 1000 ? `${(kilos / 1000).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} t` : `${kilos} kg`;
}
