import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Inscription } from './inscription';

describe('Inscription', () => {
  let api: HttpTestingController;

  async function afficherPage() {
    await TestBed.configureTestingModule({
      imports: [Inscription],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Inscription);
    api.expectOne('/api/inscription/delegations').flush([{ code: 'SAKIET_EZZIT', libelle: 'Sakiet Ezzit' }]);
    await fixture.whenStable();
    return fixture;
  }

  function remplir(page: Inscription) {
    page.formulaire.setValue({
      nom: ' Ben Salah ',
      prenom: 'Ali',
      cin: '0123 4567',
      telephone: '98 123 412',
      email: '',
      delegation: 'SAKIET_EZZIT',
      motDePasse: 'Olive2026',
      confirmationMotDePasse: 'Olive2026',
      conditionsAcceptees: true,
    });
  }

  it("n'envoie rien si le formulaire est incomplet", async () => {
    const fixture = await afficherPage();

    fixture.componentInstance.envoyer();
    await fixture.whenStable();

    api.expectNone('/api/inscription');
    expect(fixture.nativeElement.textContent).toContain('Saisissez votre nom.');
  });

  it('nettoie la saisie avant de l’envoyer (espaces de la CIN, e-mail vide)', async () => {
    const fixture = await afficherPage();
    remplir(fixture.componentInstance);

    fixture.componentInstance.envoyer();

    const corps = api.expectOne('/api/inscription').request.body;
    expect(corps.nom).toBe('Ben Salah');
    expect(corps.cin).toBe('01234567');
    expect(corps.email).toBeNull();
  });

  it('affiche sous le bon champ le refus renvoyé par le serveur', async () => {
    const fixture = await afficherPage();
    remplir(fixture.componentInstance);

    fixture.componentInstance.envoyer();
    api.expectOne('/api/inscription').flush(
      {
        statut: 409,
        code: 'DONNEES_DEJA_UTILISEES',
        message: 'Un compte existe déjà avec certaines de ces informations.',
        champs: { telephone: 'Ce numéro est déjà utilisé par un autre compte.' },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(fixture.componentInstance.erreur('telephone')).toBe('Ce numéro est déjà utilisé par un autre compte.');
    expect(fixture.nativeElement.textContent).toContain('Ce numéro est déjà utilisé par un autre compte.');
  });

  it('signale deux mots de passe différents', async () => {
    const fixture = await afficherPage();
    remplir(fixture.componentInstance);
    fixture.componentInstance.formulaire.controls.confirmationMotDePasse.setValue('Autre2026');
    fixture.componentInstance.formulaire.markAllAsTouched();

    expect(fixture.componentInstance.erreur('confirmationMotDePasse')).toBe(
      'Les deux mots de passe ne sont pas identiques.',
    );
  });
});
