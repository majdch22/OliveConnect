import { HttpClient, HttpParams } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable, tap } from 'rxjs';

import { AuthentificationService } from '../authentification/authentification.service';
import { CategorieNotification, Notification, PageNotifications } from './notification.modeles';

/**
 * Notifications de la personne connectée, pour tous les rôles.
 * - Dès qu'une session est ouverte, une connexion temps réel (WebSocket STOMP, adresse /ws) s'ouvre avec le jeton
 *   d'accès ; le serveur y pousse chaque nouvelle notification. Elle se ferme à la déconnexion.
 * - Si la connexion tombe (réseau du verger), elle se rétablit seule toutes les 5 secondes.
 * - La pastille de la cloche (nombre de non lues) et le bandeau de la dernière notification se mettent à jour seuls.
 */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly authentification = inject(AuthentificationService);
  private client: Client | null = null;

  private readonly nonLues = signal(0);
  private readonly derniere = signal<Notification | null>(null);

  /** Nombre de notifications non lues (pastille de la cloche). */
  readonly nombreNonLues = this.nonLues.asReadonly();
  /** Dernière notification reçue en temps réel (bandeau affiché quelques secondes). */
  readonly derniereRecue = this.derniere.asReadonly();

  constructor() {
    // Ouvre la connexion temps réel quand une session est active, la ferme sinon
    effect(() => {
      const session = this.authentification.session();
      if (session && !session.motDePasseProvisoire) {
        this.ouvrirTempsReel();
        this.rafraichirCompteur();
      } else {
        this.fermerTempsReel();
        this.nonLues.set(0);
      }
    });
  }

  liste(categorie: CategorieNotification | null, nonLuesSeulement: boolean, page: number): Observable<PageNotifications> {
    let parametres = new HttpParams().set('page', page).set('nonLues', nonLuesSeulement);
    if (categorie) {
      parametres = parametres.set('categorie', categorie);
    }
    return this.http.get<PageNotifications>('/api/notifications', { params: parametres });
  }

  marquerLue(notification: Notification): Observable<Notification> {
    return this.http
      .post<Notification>(`/api/notifications/${notification.id}/lue`, null)
      .pipe(tap(() => this.nonLues.update((nombre) => (notification.lue ? nombre : Math.max(0, nombre - 1)))));
  }

  toutMarquerLu(): Observable<unknown> {
    return this.http.post('/api/notifications/tout-lire', null).pipe(tap(() => this.nonLues.set(0)));
  }

  rafraichirCompteur(): void {
    this.http.get<{ nombre: number }>('/api/notifications/nombre-non-lues').subscribe({
      next: (reponse) => this.nonLues.set(reponse.nombre),
      error: () => this.nonLues.set(0),
    });
  }

  /** Ferme le bandeau de la dernière notification. */
  oublierDerniere(): void {
    this.derniere.set(null);
  }

  private ouvrirTempsReel(): void {
    if (this.client || typeof WebSocket === 'undefined') {
      return;
    }
    const securise = location.protocol === 'https:';
    this.client = new Client({
      brokerURL: `${securise ? 'wss' : 'ws'}://${location.host}/ws`,
      reconnectDelay: 5000,
      // Relit le jeton à chaque (re)connexion : il peut avoir changé depuis la première
      beforeConnect: (client) => {
        client.connectHeaders = { Authorization: `Bearer ${this.authentification.jeton() ?? ''}` };
      },
      onConnect: () => {
        this.client?.subscribe('/user/queue/notifications', (message) => {
          const notification = JSON.parse(message.body) as Notification;
          this.nonLues.update((nombre) => nombre + 1);
          this.derniere.set(notification);
        });
      },
    });
    this.client.activate();
  }

  private fermerTempsReel(): void {
    this.client?.deactivate();
    this.client = null;
  }
}
