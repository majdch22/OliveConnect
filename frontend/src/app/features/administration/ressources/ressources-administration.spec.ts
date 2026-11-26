import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Ressource } from '../administration.modeles';
import { RessourcesAdministration } from './ressources-administration';

/** Attend la fin du court délai appliqué avant chaque rechargement de la liste. */
const attendreRechargement = () => new Promise((fin) => setTimeout(fin, 250));

function ressource(changement: Partial<Ressource>): Ressource {
  return {
    id: 'r1', code: 'BN-01', type: 'BENNE', libelleType: 'Benne', nom: 'Benne 2 t', matricule: 'BEN 101',
    capaciteKg: 2000, quantite: null, equipeId: null, nomEquipe: null, telephone: null,
    etat: 'DISPONIBLE', libelleEtat: 'Disponible', archivee: false, ...changement,
  };
}

describe('RessourcesAdministration', () => {
  let api: HttpTestingController;

  async function afficherPage() {
    await TestBed.configureTestingModule({
      imports: [RessourcesAdministration],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(RessourcesAdministration);
    fixture.detectChanges();
    api.expectOne('/api/ressources/resume').flush([]);
    api.expectOne((r) => r.url === '/api/equipes').flush([]);
    await attendreRechargement();
    api.expectOne((r) => r.url === '/api/ressources').flush({ elements: [], page: 0, taille: 20, totalElements: 0, totalPages: 0 });
    return fixture.componentInstance;
  }

  it('ouvrir un onglet filtre la liste sur ce type et revient à la première page', async () => {
    const page = await afficherPage();
    page.changerFiltres({ page: 3 });

    page.choisirOnglet('BENNE');
    await attendreRechargement();

    const parametres = api.expectOne((r) => r.url === '/api/ressources').request.params;
    expect(parametres.get('type')).toBe('BENNE');
    expect(parametres.get('page')).toBe('0');
  });

  it('la colonne « Détail » montre l’information propre à chaque type', async () => {
    const page = await afficherPage();

    expect(page.detail(ressource({}))).toContain('BEN 101');
    expect(page.detail(ressource({ type: 'OUVRIER', nomEquipe: null }))).toBe('Sans équipe');
    expect(page.detail(ressource({ type: 'MATERIEL', quantite: 40 }))).toBe('40 unité(s)');
  });

  it('un ajout depuis l’onglet « Bennes » propose directement le type benne', async () => {
    const page = await afficherPage();

    page.choisirOnglet('BENNE');

    expect(page.typePourAjout()).toBe('BENNE');
  });
});
