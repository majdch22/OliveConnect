import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { FormulaireMotDePasse } from './formulaire-mot-de-passe';

describe('FormulaireMotDePasse', () => {
  it('n’envoie rien si la confirmation diffère du nouveau mot de passe', async () => {
    await TestBed.configureTestingModule({
      imports: [FormulaireMotDePasse],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    const api = TestBed.inject(HttpTestingController);
    const formulaire = TestBed.createComponent(FormulaireMotDePasse).componentInstance;
    formulaire.formulaire.setValue({ motDePasseActuel: 'Ancien2026', nouveauMotDePasse: 'Nouveau2026', confirmation: 'Autre2026' });

    formulaire.envoyer();

    api.expectNone('/api/profil/mot-de-passe');
    expect(formulaire.erreur('confirmation')).toBe('Les deux mots de passe ne sont pas identiques.');
  });
});
