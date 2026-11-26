import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { ConnexionReponse, EspaceConnexion, pageDeConnexion, Session } from './modeles';

/** Clé de la session dans le stockage du navigateur. */
const CLE_SESSION = 'oliveconnect.session';

/**
 * Connexion, déconnexion et session en cours.
 * La session (jeton + rôle) est gardée dans le navigateur pour survivre à un rechargement de la page.
 */
@Injectable({ providedIn: 'root' })
export class AuthentificationService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessionCourante = signal<Session | null>(lireSessionEnregistree());

  /** Session en cours, ou null si personne n'est connecté. */
  readonly session = this.sessionCourante.asReadonly();
  readonly estConnecte = computed(() => this.sessionCourante() !== null);

  connecter(identifiant: string, motDePasse: string, espace: EspaceConnexion): Observable<ConnexionReponse> {
    const adresse = espace === 'ADMINISTRATION' ? '/api/administration/connexion' : '/api/connexion';
    return this.http
      .post<ConnexionReponse>(adresse, { identifiant, motDePasse })
      .pipe(tap((reponse) => this.enregistrerSession(reponse)));
  }

  /** Après un changement de mot de passe : il n'est plus provisoire, l'espace devient accessible. */
  noterMotDePasseChange(): void {
    const session = this.sessionCourante();
    if (session) {
      this.enregistrerSession({ ...session, motDePasseProvisoire: false });
    }
  }

  /**
   * Ferme la session et renvoie vers la page de connexion du rôle.
   * @param sessionExpiree vrai quand le serveur a refusé le jeton : la page de connexion l'explique
   */
  deconnecter(sessionExpiree = false): void {
    const role = this.sessionCourante()?.role ?? null;
    this.effacerSession();
    this.router.navigate([pageDeConnexion(role)], { queryParams: sessionExpiree ? { sessionExpiree: 1 } : {} });
  }

  /** Jeton à joindre aux requêtes, ou null s'il est absent ou expiré. */
  jeton(): string | null {
    const session = this.sessionCourante();
    if (session && !estValide(session)) {
      this.effacerSession();
      return null;
    }
    return session?.jeton ?? null;
  }

  private enregistrerSession(session: Session): void {
    this.sessionCourante.set(session);
    try {
      localStorage.setItem(CLE_SESSION, JSON.stringify(session));
    } catch {
      // Stockage indisponible (navigation privée) : la session reste valable jusqu'au rechargement.
    }
  }

  private effacerSession(): void {
    this.sessionCourante.set(null);
    try {
      localStorage.removeItem(CLE_SESSION);
    } catch {
      // Rien à effacer si le stockage est indisponible.
    }
  }
}

function estValide(session: Session): boolean {
  return new Date(session.expiration).getTime() > Date.now();
}

/** Relit la session enregistrée ; une session expirée ou illisible est ignorée. */
function lireSessionEnregistree(): Session | null {
  try {
    const texte = localStorage.getItem(CLE_SESSION);
    const session = texte ? (JSON.parse(texte) as Session) : null;
    return session && estValide(session) ? session : null;
  } catch {
    return null;
  }
}
