import { Role } from '../../core/authentification/modeles';
import { StatutCompte } from '../../shared/ui/badge-statut';

/** Un compte vu par l'administrateur (miroir de CompteAdministration en Java). */
export interface CompteAdministration {
  id: string;
  nom: string;
  prenom: string;
  email: string | null;
  telephone: string;
  role: Role;
  libelleRole: string;
  statut: StatutCompte;
  motDePasseProvisoire: boolean;
  dateCreation: string | null;
  derniereConnexion: string | null;
  cin: string | null;
  delegation: string | null;
  libelleDelegation: string | null;
  numeroAdherent: string | null;
  dateTraitementInscription: string | null;
  motifRefus: string | null;
  pressoirId: string | null;
}

/** Une page de résultats (miroir de PageResultat en Java). */
export interface PageResultat<T> {
  elements: T[];
  page: number;
  taille: number;
  totalElements: number;
  totalPages: number;
}

/** Colonnes de tri (miroir de TriComptes en Java). */
export type TriComptes = 'NOM' | 'ROLE' | 'STATUT' | 'DATE_CREATION' | 'DERNIERE_CONNEXION';
export type Sens = 'ASC' | 'DESC';

/** Filtres, tri et pagination de la liste des utilisateurs. Une chaîne vide signifie « tous ». */
export interface FiltresComptes {
  texte: string;
  role: Role | '';
  statut: StatutCompte | '';
  delegation: string;
  tri: TriComptes;
  sens: Sens;
  page: number;
  taille: number;
}

/** Onglets de la page des inscriptions (miroir de EtatInscription en Java). */
export type EtatInscription = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

export interface FiltresInscriptions {
  etat: EtatInscription;
  texte: string;
  delegation: string;
  tri: TriComptes;
  sens: Sens;
  page: number;
  taille: number;
}

/** Formulaire de création / modification d'un compte (miroir de CompteRequete en Java). */
export interface CompteRequete {
  nom: string;
  prenom: string;
  email: string | null;
  telephone: string;
  role: Role;
  pressoirId: string | null;
}

/** Compte accompagné de son mot de passe provisoire, montré une seule fois. */
export interface CompteEtMotDePasse {
  compte: CompteAdministration;
  motDePasseProvisoire: string;
}

/** Chiffres de l'accueil de l'administration (miroir de StatistiquesComptes en Java). */
export interface StatistiquesComptes {
  inscriptionsEnAttente: number;
  inscriptionsRefusees: number;
  comptesActifs: number;
  comptesDesactives: number;
  totalComptes: number;
  parRole: { role: Role; libelle: string; nombre: number }[];
}

/** Une valeur proposée dans une liste déroulante. */
export interface OptionListe {
  code: string;
  libelle: string;
}

/** Rôles proposés dans les filtres et les formulaires, avec leur libellé. */
export const ROLES: { code: Role; libelle: string }[] = [
  { code: 'AGRICULTEUR', libelle: 'Agriculteur' },
  { code: 'RESPONSABLE_COLLECTE', libelle: 'Responsable de collecte' },
  { code: 'CHEF_EQUIPE', libelle: "Chef d'équipe de récolte" },
  { code: 'RESPONSABLE_LOGISTIQUE', libelle: 'Responsable logistique' },
  { code: 'RESPONSABLE_PRESSOIR', libelle: 'Responsable du pressoir' },
  { code: 'ADMINISTRATEUR', libelle: 'Administrateur' },
];

/** États proposés dans le filtre de la liste des utilisateurs. */
export const STATUTS: { code: StatutCompte; libelle: string }[] = [
  { code: 'ACTIF', libelle: 'Actif' },
  { code: 'EN_ATTENTE_VALIDATION', libelle: 'En attente' },
  { code: 'DESACTIVE', libelle: 'Désactivé' },
  { code: 'REFUSE', libelle: 'Refusé' },
];

// --- Supervision (miroir de SupervisionService.EtatServeur en Java) ---

export type EtatComposant = 'OPERATIONNEL' | 'ATTENTION' | 'HORS_SERVICE';

export interface Composant {
  nom: string;
  etat: EtatComposant;
  libelleEtat: string;
  detail: string;
}

export interface Echantillon {
  moment: string;
  processeur: number;
  memoireUtiliseeMo: number;
  requetesParMinute: number;
  tempsReponseMoyenMs: number | null;
  latenceBaseMs: number | null;
}

export interface EvenementJournal {
  moment: string;
  niveau: 'AVERTISSEMENT' | 'ERREUR';
  origine: string;
  message: string;
}

export interface Sauvegarde {
  nom: string;
  tailleOctets: number;
  date: string;
}

export interface EtatServeur {
  genereLe: string;
  demarreLe: string;
  dureeFonctionnementSecondes: number;
  adresseGrafana: string;
  composants: Composant[];
  indicateurs: {
    processeur: number;
    memoireUtiliseeMo: number;
    memoireMaximaleMo: number;
    requetesParMinute: number;
    tempsReponseMoyenMs: number | null;
    utilisateursActifs: number;
  };
  historique: Echantillon[];
  evenements: EvenementJournal[];
  sauvegardes: Sauvegarde[];
}

// --- Paramètres (miroir de Parametres en Java) ---

export type FrequenceImport =
  | 'TOUTES_LES_5_MINUTES'
  | 'TOUTES_LES_15_MINUTES'
  | 'TOUTES_LES_30_MINUTES'
  | 'TOUTES_LES_HEURES'
  | 'CHAQUE_JOUR';

export interface Parametres {
  campagne: { debut: string; fin: string };
  regles: {
    seuilAlerteMaturite: number;
    tolerancePesee: number;
    distanceMaximaleSignalement: number;
    delaiCibleUrgences: number;
  };
  imports: { referentiels: FrequenceImport; meteoEtMaturite: FrequenceImport; capacitePressoirs: FrequenceImport };
  notifications: { smsAgriculteurs: boolean; emailRecapitulatifQuotidien: boolean };
  securite: { dureeSessionHeures: number; tentativesAvantBlocage: number; dureeBlocageMinutes: number };
  modifieLe?: string;
  modifiePar?: string;
}

export interface ParametresReponse {
  parametres: Parametres;
  frequencesPossibles: { code: FrequenceImport; libelle: string }[];
}

// --- Référentiels ---

export interface Variete {
  id: string;
  code: string;
  nom: string;
  rendementMoyenKgParHectare: number;
  moisDebutRecolte: number;
  moisFinRecolte: number;
  saison: string;
  archivee: boolean;
}

export interface VarieteRequete {
  nom: string;
  rendementMoyenKgParHectare: number;
  moisDebutRecolte: number;
  moisFinRecolte: number;
}

export interface Pressoir {
  id: string;
  code: string;
  nom: string;
  delegation: string;
  libelleDelegation: string;
  latitude: number;
  longitude: number;
  capaciteJournaliereKg: number;
  telephone: string | null;
  archive: boolean;
  nombreResponsables: number;
}

export interface PressoirRequete {
  nom: string;
  delegation: string;
  latitude: number;
  longitude: number;
  capaciteJournaliereKg: number;
  telephone: string | null;
}

export interface CategorieQualite {
  code: string;
  libelle: string;
  description: string;
}

/** Filtres des listes de référentiels (courtes, non paginées). */
export interface FiltresReferentiel {
  texte: string;
  avecArchives: boolean;
  tri: string;
  sens: Sens;
}

// --- Ressources (miroir du paquet « ressource » en Java) ---

export type TypeRessource = 'TRACTEUR' | 'BENNE' | 'OUVRIER' | 'MATERIEL';
export type EtatRessource = 'DISPONIBLE' | 'EN_PANNE' | 'EN_REVISION' | 'EN_CONGE';

export interface Ressource {
  id: string;
  code: string;
  type: TypeRessource;
  libelleType: string;
  nom: string;
  matricule: string | null;
  capaciteKg: number | null;
  quantite: number | null;
  equipeId: string | null;
  nomEquipe: string | null;
  telephone: string | null;
  etat: EtatRessource;
  libelleEtat: string;
  archivee: boolean;
}

export interface RessourceRequete {
  type: TypeRessource;
  nom: string;
  matricule: string | null;
  capaciteKg: number | null;
  quantite: number | null;
  equipeId: string | null;
  telephone: string | null;
  etat: EtatRessource;
}

export interface ResumeRessources {
  type: TypeRessource;
  libelle: string;
  total: number;
  disponibles: number;
}

export interface Equipe {
  id: string;
  code: string;
  nom: string;
  nombreMembres: number;
  nombreDisponibles: number;
  archivee: boolean;
}

export interface FiltresRessources {
  texte: string;
  type: TypeRessource | '';
  etat: EtatRessource | '';
  equipeId: string;
  avecArchives: boolean;
  tri: string;
  sens: Sens;
  page: number;
  taille: number;
}

/** Types de ressources avec leur libellé au pluriel (onglets) et au singulier (formulaire). */
export const TYPES_RESSOURCES: { code: TypeRessource; pluriel: string; singulier: string }[] = [
  { code: 'TRACTEUR', pluriel: 'Tracteurs', singulier: 'Tracteur' },
  { code: 'BENNE', pluriel: 'Bennes', singulier: 'Benne' },
  { code: 'OUVRIER', pluriel: 'Ouvriers saisonniers', singulier: 'Ouvrier saisonnier' },
  { code: 'MATERIEL', pluriel: 'Matériel', singulier: 'Matériel' },
];

/** États d'une ressource, avec la couleur de leur pastille. */
export const ETATS_RESSOURCES: { code: EtatRessource; libelle: string; classes: string }[] = [
  { code: 'DISPONIBLE', libelle: 'Disponible', classes: 'bg-olive-100 text-olive-900' },
  { code: 'EN_PANNE', libelle: 'En panne', classes: 'bg-red-100 text-red-800' },
  { code: 'EN_REVISION', libelle: 'En révision', classes: 'bg-amber-100 text-amber-800' },
  { code: 'EN_CONGE', libelle: 'En congé', classes: 'bg-gray-200 text-gray-700' },
];

// --- Carte (miroir de CarteService.DonneesCarte en Java) ---

export interface DelegationSurCarte {
  code: string;
  libelle: string;
  latitude: number;
  longitude: number;
  agriculteursActifs: number;
  inscriptionsEnAttente: number;
}

export interface DonneesCarte {
  pressoirs: Pressoir[];
  delegations: DelegationSurCarte[];
}
