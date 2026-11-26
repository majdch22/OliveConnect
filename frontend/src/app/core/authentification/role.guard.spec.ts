import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router, UrlTree } from '@angular/router';

import { AuthentificationService } from './authentification.service';
import { Session } from './modeles';
import { roleGuard } from './role.guard';

function session(changement: Partial<Session>): Session {
  return {
    id: 'compte-1', jeton: 'jeton', expiration: new Date(Date.now() + 3_600_000).toISOString(), role: 'AGRICULTEUR',
    libelleRole: 'Agriculteur', prenom: 'Ali', nom: 'Ben Salah', motDePasseProvisoire: false, ...changement,
  };
}

describe('roleGuard', () => {
  function verifier(sessionActuelle: Session | null) {
    localStorage.clear();
    if (sessionActuelle) {
      localStorage.setItem('oliveconnect.session', JSON.stringify(sessionActuelle));
    }
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
    TestBed.inject(AuthentificationService);
    const resultat = TestBed.runInInjectionContext(() => roleGuard('AGRICULTEUR')({} as never, {} as never));
    return resultat instanceof UrlTree ? TestBed.inject(Router).serializeUrl(resultat) : resultat;
  }

  it('sans session, renvoie vers la page de connexion', () => {
    expect(verifier(null)).toBe('/connexion');
  });

  it('avec un autre rôle, renvoie vers l’espace de ce rôle', () => {
    expect(verifier(session({ role: 'CHEF_EQUIPE' }))).toBe('/chef-equipe');
  });

  it('avec un mot de passe provisoire, renvoie vers le changement de mot de passe', () => {
    expect(verifier(session({ motDePasseProvisoire: true }))).toBe('/mot-de-passe');
  });

  it('avec le bon rôle et son propre mot de passe, laisse entrer', () => {
    expect(verifier(session({}))).toBe(true);
  });
});
