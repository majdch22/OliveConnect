import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MotDePasseOublie } from './mot-de-passe-oublie';

describe('MotDePasseOublie', () => {
  let api: HttpTestingController;
  let page: MotDePasseOublie;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MotDePasseOublie],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    page = TestBed.createComponent(MotDePasseOublie).componentInstance;
  });

  afterEach(() => page.ngOnDestroy());

  it('après l’envoi du code, passe à l’étape 2 et bloque le renvoi pendant une minute', () => {
    page.identifiant.setValue('98123412');
    page.demanderCode();
    api.expectOne('/api/mot-de-passe-oublie/code').flush({ message: 'Code envoyé.', delaiRenvoiSecondes: 60, dureeValiditeMinutes: 10 });

    expect(page.etape()).toBe(2);
    expect(page.texteAttente()).toBe('1:00');
  });

  it('un code faux affiche le nombre d’essais restants sous le champ', () => {
    page.identifiant.setValue('98123412');
    page.etape.set(2);
    page.code.setValue('000000');

    page.verifierCode();
    api.expectOne('/api/mot-de-passe-oublie/verification').flush(
      { code: 'CODE_INCORRECT', message: 'Code incorrect. Il vous reste 2 essais.', champs: { code: 'Code incorrect. Il vous reste 2 essais.' } },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(page.etape()).toBe(2);
    expect(page.erreur('code')).toBe('Code incorrect. Il vous reste 2 essais.');
  });

  it('deux mots de passe différents ne partent pas au serveur', () => {
    page.etape.set(3);
    page.nouveau.setValue({ nouveauMotDePasse: 'Nouveau2026', confirmation: 'Autre2026' });

    page.reinitialiser();

    api.expectNone('/api/mot-de-passe-oublie/nouveau');
    expect(page.erreur('confirmation')).toBe('Les deux mots de passe ne sont pas identiques.');
  });
});
