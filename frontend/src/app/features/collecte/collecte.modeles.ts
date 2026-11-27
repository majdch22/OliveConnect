/** Modèles de l'espace du responsable de collecte (miroirs des classes Java du même nom). */

import { StatutDemande, StatutRecolte, TypeAlerte, TypeRecolte } from '../agriculteur/agriculteur.modeles';

export type { StatutDemande, TypeAlerte, TypeRecolte } from '../agriculteur/agriculteur.modeles';

export interface Page<T> {
  elements: T[];
  page: number;
  taille: number;
  totalElements: number;
  totalPages: number;
}

// --- Demandes ---

export interface DemandeCollecte {
  id: string;
  numero: string;
  type: TypeRecolte;
  libelleType: string;
  statut: StatutDemande;
  libelleStatut: string;
  dateSouhaitee: string;
  datePlanifiee: string | null;
  tourneeId: string | null;
  numeroTournee: string | null;
  quantiteEstimeeKg: number;
  commentaire: string | null;
  motifRejet: string | null;
  motifAnnulation: string | null;
  dateSoumission: string;
  creeeParLaCooperative: boolean;
  vergerId: string;
  nomVerger: string;
  codeVerger: string | null;
  libelleDelegation: string | null;
  nomVariete: string | null;
  superficieHa: number;
  nombreArbres: number;
  niveauMaturite: number | null;
  libelleStatutRecolte: string | null;
  latitude: number;
  longitude: number;
  agriculteurId: string;
  nomAgriculteur: string;
  numeroAdherent: string | null;
  telephoneAgriculteur: string | null;
  historique: { libelleNouveau: string; date: string; nomAuteur: string | null; commentaire: string | null }[];
}

export interface FicheDemande {
  demande: DemandeCollecte;
  pressoirLePlusProche: string | null;
  distancePressoirKm: number | null;
  collectesTermineesDuVerger: number;
  signalementsNonResolusDuVerger: number;
}

/** Nombre de demandes par état, plus URGENTES_A_TRAITER. */
export type CompteursDemandes = Record<StatutDemande | 'URGENTES_A_TRAITER', number>;

// --- Planification et tournées ---

export type TypeRessource = 'TRACTEUR' | 'BENNE' | 'OUVRIER' | 'MATERIEL';
export type StatutTournee = 'PLANIFIEE' | 'EN_COURS' | 'LIVREE' | 'CLOTUREE' | 'ANNULEE';

export interface ChefDisponible {
  id: string;
  nomComplet: string;
  telephone: string;
}

export interface RessourceDisponible {
  id: string;
  code: string;
  type: TypeRessource;
  libelleType: string;
  nom: string;
  capaciteKg: number | null;
  quantite: number | null;
  equipeId: string | null;
  nomEquipe: string | null;
}

export interface EquipeDisponible {
  id: string;
  code: string;
  nom: string;
  effectif: number;
  ouvriersDisponibles: string[];
}

export interface PressoirDisponible {
  id: string;
  code: string;
  nom: string;
  libelleDelegation: string;
  latitude: number;
  longitude: number;
  capaciteJournaliereKg: number;
  reserveKg: number;
  restantKg: number;
}

export interface OptionsPlanification {
  date: string;
  chefs: ChefDisponible[];
  chefsOccupes: number;
  ressources: RessourceDisponible[];
  ressourcesOccupees: number;
  equipes: EquipeDisponible[];
  pressoirs: PressoirDisponible[];
}

export interface PlanificationRequete {
  date: string;
  demandeIds: string[];
  chefEquipeId: string;
  pressoirId: string;
  ressourceIds: string[];
  commentaire: string | null;
}

export interface Verification {
  conflits: string[];
  avertissements: string[];
  quantiteEstimeeKg: number;
  capaciteBennesKg: number;
  pressoir: PressoirDisponible;
  pressoirsSuggeres: PressoirDisponible[];
}

export interface EtapeTournee {
  ordre: number;
  demandeId: string;
  numeroDemande: string;
  vergerId: string;
  nomVerger: string;
  agriculteurId: string;
  nomAgriculteur: string;
  type: TypeRecolte;
  quantiteEstimeeKg: number;
}

export interface Affectation {
  ressourceId: string;
  type: TypeRessource;
  code: string;
  nom: string;
  capaciteKg: number | null;
  nomEquipe: string | null;
}

export interface Tournee {
  id: string;
  numero: string;
  date: string;
  statut: StatutTournee;
  libelleStatut: string;
  chefEquipeId: string;
  nomChefEquipe: string;
  pressoirId: string;
  nomPressoir: string;
  etapes: EtapeTournee[];
  affectations: Affectation[];
  quantiteEstimeeKg: number;
  capaciteBennesKg: number;
  commentaire: string | null;
  motifAnnulation: string | null;
  annulable: boolean;
  dateCreation: string;
  historique: { libelleNouveau: string; date: string; commentaire: string | null }[];
}

// --- Signalements ---

export type StatutAlerte = 'OUVERTE' | 'PRISE_EN_CHARGE' | 'RESOLUE';

export interface AlerteCollecte {
  id: string;
  numero: string;
  type: TypeAlerte;
  libelleType: string;
  statut: StatutAlerte;
  libelleStatut: string;
  dateCreation: string;
  description: string;
  avecPhoto: boolean;
  latitude: number;
  longitude: number;
  distanceAuVergerMetres: number;
  loinDuVerger: boolean;
  vergerId: string;
  nomVerger: string;
  niveauMaturite: number | null;
  agriculteurId: string;
  nomAgriculteur: string;
  telephoneAgriculteur: string | null;
  demandeUrgenteId: string | null;
  numeroDemandeUrgente: string | null;
  datePriseEnCharge: string | null;
  dateResolution: string | null;
  traitements: { libelleStatut: string; date: string; nomAuteur: string | null; commentaire: string | null }[];
}

export type CompteursAlertes = Record<StatutAlerte, number>;

// --- Carte ---

export interface VergerSurCarte {
  id: string;
  code: string;
  nom: string;
  latitude: number;
  longitude: number;
  nomAgriculteur: string;
  nomVariete: string;
  superficieHa: number;
  niveauMaturite: number | null;
  statutRecolte: StatutRecolte;
  libelleStatutRecolte: string;
  demandesEnCours: number;
  signalementsNonResolus: number;
  tourneeDuJour: string | null;
}

export interface AlerteSurCarte {
  id: string;
  numero: string;
  type: TypeAlerte;
  libelleType: string;
  statut: StatutAlerte;
  libelleStatut: string;
  latitude: number;
  longitude: number;
  nomVerger: string;
  description: string;
}

export interface PressoirSurCarte {
  id: string;
  code: string;
  nom: string;
  libelleDelegation: string;
  latitude: number;
  longitude: number;
  capaciteJournaliereKg: number;
}

export interface DonneesCarteCollecte {
  vergers: VergerSurCarte[];
  alertes: AlerteSurCarte[];
  pressoirs: PressoirSurCarte[];
}

// --- Tableau de bord ---

export interface TableauDeBordCollecte {
  demandesParStatut: CompteursDemandes;
  alertesParStatut: CompteursAlertes;
  alertesNonResoluesParType: { type: TypeAlerte; libelle: string; nombre: number }[];
  delaiMoyenPriseEnChargeMinutes: number | null;
  tourneesDuJourParStatut: Partial<Record<StatutTournee, number>>;
  quantitePlanifieeAujourdhuiKg: number;
  charge: { date: string; planifieeKg: number; enAttenteKg: number; tournees: number }[];
  pressoirsAujourdhui: PressoirDisponible[];
}

// --- Apparence ---

export const COULEURS_STATUT_TOURNEE: Record<StatutTournee, string> = {
  PLANIFIEE: 'bg-olive-100 text-olive-900',
  EN_COURS: 'bg-olive-700 text-white',
  LIVREE: 'bg-sky-100 text-sky-800',
  CLOTUREE: 'bg-gray-200 text-gray-700',
  ANNULEE: 'bg-red-100 text-red-800',
};

export const COULEURS_STATUT_ALERTE: Record<StatutAlerte, string> = {
  OUVERTE: 'bg-red-100 text-red-800',
  PRISE_EN_CHARGE: 'bg-amber-100 text-amber-800',
  RESOLUE: 'bg-olive-100 text-olive-900',
};

export const ICONES_RESSOURCE: Record<TypeRessource, string> = {
  TRACTEUR: '🚜',
  BENNE: '🧺',
  OUVRIER: '👷',
  MATERIEL: '🧰',
};

/** Motifs proposés en un clic pour rejeter une demande (maquette RC1). */
export const MOTIFS_REJET = [
  'Verger pas encore mûr : renouvelez la demande dans quelques jours.',
  'Demande en double.',
  'Verger hors de la zone de collecte.',
];

/** Date du jour au format AAAA-MM-JJ, dans le fuseau du navigateur. */
export function aujourdhui(decalageJours = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + decalageJours);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
