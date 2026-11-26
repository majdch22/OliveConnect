/** Modèles des notifications (miroirs de NotificationReponse et CategorieNotification en Java). */

export type CategorieNotification = 'COLLECTE' | 'ALERTE' | 'COMPTE';

export interface Notification {
  id: string;
  categorie: CategorieNotification;
  libelleCategorie: string;
  titre: string;
  message: string;
  /** Page de l'application à ouvrir en touchant la notification, ou null. */
  lien: string | null;
  prioritaire: boolean;
  lue: boolean;
  dateCreation: string;
}

/** Une page de la liste (miroir de PageResultat en Java). */
export interface PageNotifications {
  elements: Notification[];
  page: number;
  taille: number;
  totalElements: number;
  totalPages: number;
}
