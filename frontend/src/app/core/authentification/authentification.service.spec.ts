import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthentificationService } from './authentification.service';
import { ConnexionReponse } from './modeles';

/** Réponse du serveur simulée, valable une heure. */
function reponseAgriculteur(): ConnexionReponse {
  return {
    jeton: 'jeton-signe',
    expiration: new Date(Date.now() + 3_600_000).toISOString(),
    role: 'AGRICULTEUR',
    libelleRole: 'Agriculteur',
    prenom: 'Ali',
    nom: 'Ben Salah',
  };
}

describe('AuthentificationService', () => {
  let service: AuthentificationService;
  let api: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthentificationService);
    api = TestBed.inject(HttpTestingController);
  });

  afterEach(() => api.verify());

  it('une connexion réussie ouvre la session et garde le jeton', () => {
    service.connecter('ali@exemple.tn', 'Olive2026!', 'UTILISATEURS').subscribe();

    const requete = api.expectOne('/api/connexion');
    expect(requete.request.body).toEqual({ identifiant: 'ali@exemple.tn', motDePasse: 'Olive2026!' });
    requete.flush(reponseAgriculteur());

    expect(service.estConnecte()).toBe(true);
    expect(service.jeton()).toBe('jeton-signe');
  });

  it("la page administrateur appelle l'adresse de connexion administrateur", () => {
    service.connecter('administrateur@oliveconnect.tn', 'secret', 'ADMINISTRATION').subscribe();

    api.expectOne('/api/administration/connexion').flush({ ...reponseAgriculteur(), role: 'ADMINISTRATEUR' });

    expect(service.session()?.role).toBe('ADMINISTRATEUR');
  });

  it('un jeton expiré est considéré comme absent', () => {
    service.connecter('ali@exemple.tn', 'Olive2026!', 'UTILISATEURS').subscribe();
    api.expectOne('/api/connexion').flush({ ...reponseAgriculteur(), expiration: '2000-01-01T00:00:00Z' });

    expect(service.jeton()).toBeNull();
    expect(service.estConnecte()).toBe(false);
  });
});
