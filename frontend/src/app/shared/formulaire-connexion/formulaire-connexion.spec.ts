import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { FormulaireConnexion } from './formulaire-connexion';

describe('FormulaireConnexion', () => {
  let api: HttpTestingController;

  async function afficherFormulaire() {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [FormulaireConnexion],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(FormulaireConnexion);
    fixture.componentRef.setInput('espace', 'UTILISATEURS');
    await fixture.whenStable();
    return fixture;
  }

  it("n'appelle pas le serveur si les champs sont vides et affiche les messages", async () => {
    const fixture = await afficherFormulaire();

    fixture.componentInstance.envoyer();
    await fixture.whenStable();

    api.expectNone('/api/connexion');
    expect(fixture.nativeElement.textContent).toContain('Saisissez votre mot de passe.');
  });

  it("affiche le message d'erreur renvoyé par le serveur", async () => {
    const fixture = await afficherFormulaire();
    fixture.componentInstance.formulaire.setValue({ identifiant: 'ali@exemple.tn', motDePasse: 'mauvais' });

    fixture.componentInstance.envoyer();
    api.expectOne('/api/connexion').flush(
      { statut: 401, code: 'IDENTIFIANTS_INCORRECTS', message: 'Identifiant ou mot de passe incorrect.', champs: {} },
      { status: 401, statusText: 'Unauthorized' },
    );
    await fixture.whenStable();

    const alerte = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alerte.textContent).toContain('Identifiant ou mot de passe incorrect.');
  });
});
