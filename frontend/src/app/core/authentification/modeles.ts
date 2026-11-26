/** Rôles de la plateforme (mêmes noms que l'énumération Role côté Java). */
export type Role =
  | 'AGRICULTEUR'
  | 'RESPONSABLE_COLLECTE'
  | 'CHEF_EQUIPE'
  | 'RESPONSABLE_LOGISTIQUE'
  | 'RESPONSABLE_PRESSOIR'
  | 'ADMINISTRATEUR';

/** Les deux pages de connexion : générale, ou réservée à l'administrateur. */
export type EspaceConnexion = 'UTILISATEURS' | 'ADMINISTRATION';

/** Réponse du serveur après une connexion réussie (miroir de ConnexionReponse en Java). */
export interface ConnexionReponse {
  id: string;
  jeton: string;
  expiration: string;
  role: Role;
  libelleRole: string;
  prenom: string;
  nom: string;
  /** Vrai si le mot de passe a été donné par l'administrateur : il faut le changer avant d'utiliser son espace. */
  motDePasseProvisoire: boolean;
}

/** Session gardée dans le navigateur tant que le jeton est valide. */
export type Session = ConnexionReponse;

/** Format des erreurs renvoyées par le serveur (miroir de ReponseErreur en Java). */
export interface ReponseErreur {
  statut: number;
  code: string;
  message: string;
  champs: Record<string, string>;
}

/** Adresse de l'espace ouvert après connexion, pour chaque rôle. */
export const ESPACE_PAR_ROLE: Record<Role, string> = {
  AGRICULTEUR: '/agriculteur',
  RESPONSABLE_COLLECTE: '/collecte',
  CHEF_EQUIPE: '/chef-equipe',
  RESPONSABLE_LOGISTIQUE: '/logistique',
  RESPONSABLE_PRESSOIR: '/pressoir',
  ADMINISTRATEUR: '/administration',
};

/** Libellé de chaque rôle, tel qu'affiché à l'écran. */
export const LIBELLES_ROLE: Record<Role, string> = {
  AGRICULTEUR: 'Agriculteur',
  RESPONSABLE_COLLECTE: 'Responsable de collecte',
  CHEF_EQUIPE: "Chef d'équipe de récolte",
  RESPONSABLE_LOGISTIQUE: 'Responsable logistique',
  RESPONSABLE_PRESSOIR: 'Responsable du pressoir',
  ADMINISTRATEUR: 'Administrateur',
};

/** Page de connexion à utiliser pour un rôle donné. */
export function pageDeConnexion(role: Role | null): string {
  return role === 'ADMINISTRATEUR' ? '/administration/connexion' : '/connexion';
}
