import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FormulaireDemande } from './formulaire-demande';

describe('FormulaireDemande', () => {
  let api: HttpTestingController;

  async function afficher() {
    await TestBed.configureTestingModule({
      imports: [FormulaireDemande],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(FormulaireDemande);
    fixture.detectChanges();
    api.expectOne('/api/agriculteur/vergers').flush([]);
    return fixture.componentInstance;
  }

  it('une récolte urgente peut être pour aujourd’hui, une récolte planifiée seulement à partir de demain', async () => {
    const formulaire = await afficher();
    const demainParDefaut = formulaire.datePlusTot();

    formulaire.choisirType('URGENTE');

    expect(formulaire.datePlusTot() < demainParDefaut).toBe(true);
  });

  it('choisir un verger demande son estimation, et la suggestion remplit la quantité en un clic', async () => {
    const formulaire = await afficher();

    formulaire.formulaire.controls.vergerId.setValue('verger-1');
    api.expectOne('/api/agriculteur/vergers/verger-1/estimation').flush({ quantiteKg: 13500 });
    formulaire.utiliserEstimation(formulaire.estimation()!.quantiteKg);

    expect(formulaire.formulaire.controls.quantiteEstimeeKg.value).toBe(13500);
  });

  it('un formulaire incomplet n’est pas envoyé', async () => {
    const formulaire = await afficher();

    formulaire.envoyer();

    api.expectNone('/api/agriculteur/demandes');
    expect(formulaire.erreur('vergerId')).toBe('Choisissez le verger.');
  });
});
