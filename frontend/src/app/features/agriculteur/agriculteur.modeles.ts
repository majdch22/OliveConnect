/** Modèles de l'espace agriculteur (miroirs des classes Java du même nom). */

export type StatutRecolte = 'NON_MUR' | 'MUR' | 'EN_RECOLTE' | 'RECOLTE';

export interface Verger {
  id: string;
  code: string;
  nom: string;
  latitude: number;
  longitude: number;
  delegation: string;
  libelleDelegation: string;
  superficieHa: number;
  nombreArbres: number;
  varieteId: string;
  nomVariete: string;
  niveauMaturite: number | null;
  statutRecolte: StatutRecolte;
  libelleStatutRecolte: string;
  archive: boolean;
  /** Signalements du verger qui attendent encore une réponse de la coopérative. */
  signalementsNonResolus: number;
}

export interface VergerRequete {
  nom: string;
  latitude: number;
  longitude: number;
  delegation: string;
  superficieHa: number;
  nombreArbres: number;
  varieteId: string;
}

export type TypeRecolte = 'PLANIFIEE' | 'URGENTE';
export type StatutDemande = 'SOUMISE' | 'VALIDEE' | 'PLANIFIEE' | 'EN_COURS' | 'TERMINEE' | 'REJETEE' | 'ANNULEE';

export interface Demande {
  id: string;
  numero: string;
  vergerId: string;
  nomVerger: string;
  type: TypeRecolte;
  libelleType: string;
  dateSouhaitee: string;
  quantiteEstimeeKg: number;
  commentaire: string | null;
  statut: StatutDemande;
  libelleStatut: string;
  modifiable: boolean;
  dateSoumission: string;
  motifAnnulation: string | null;
  historique: { libelleAncien: string | null; libelleNouveau: string; date: string; commentaire: string | null }[];
}

export interface DemandeRequete {
  vergerId: string;
  type: TypeRecolte;
  dateSouhaitee: string;
  quantiteEstimeeKg: number;
  commentaire: string | null;
}

export type TypeAlerte = 'MATURITE' | 'PLUIE' | 'RAVAGEUR' | 'AUTRE';

export interface Signalement {
  id: string;
  numero: string;
  vergerId: string;
  nomVerger: string;
  type: TypeAlerte;
  libelleType: string;
  latitude: number;
  longitude: number;
  distanceAuVergerMetres: number;
  loinDuVerger: boolean;
  description: string;
  avecPhoto: boolean;
  statut: 'OUVERTE' | 'PRISE_EN_CHARGE' | 'RESOLUE';
  libelleStatut: string;
  dateCreation: string;
}

export interface SignalementRequete {
  vergerId: string;
  type: TypeAlerte;
  latitude: number;
  longitude: number;
  description: string;
}

/** Les étapes normales d'une demande, dans l'ordre, pour la frise d'avancement. */
export const ETAPES_DEMANDE: { statut: StatutDemande; libelle: string }[] = [
  { statut: 'SOUMISE', libelle: 'Soumise' },
  { statut: 'VALIDEE', libelle: 'Validée' },
  { statut: 'PLANIFIEE', libelle: 'Planifiée' },
  { statut: 'EN_COURS', libelle: 'En cours' },
  { statut: 'TERMINEE', libelle: 'Terminée' },
];

/** Couleur de la pastille de chaque état de demande. */
export const COULEURS_STATUT_DEMANDE: Record<StatutDemande, string> = {
  SOUMISE: 'bg-amber-100 text-amber-800',
  VALIDEE: 'bg-sky-100 text-sky-800',
  PLANIFIEE: 'bg-olive-100 text-olive-900',
  EN_COURS: 'bg-olive-700 text-white',
  TERMINEE: 'bg-gray-200 text-gray-700',
  REJETEE: 'bg-red-100 text-red-800',
  ANNULEE: 'bg-gray-100 text-gray-500',
};

export const TYPES_SIGNALEMENT: { code: TypeAlerte; libelle: string; icone: string }[] = [
  { code: 'MATURITE', libelle: 'Maturité', icone: '🫒' },
  { code: 'PLUIE', libelle: 'Pluie', icone: '🌧' },
  { code: 'RAVAGEUR', libelle: 'Ravageur', icone: '🐛' },
  { code: 'AUTRE', libelle: 'Autre', icone: '❔' },
];
