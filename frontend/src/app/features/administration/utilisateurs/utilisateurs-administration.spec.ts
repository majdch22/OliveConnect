import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { UtilisateursAdministration } from './utilisateurs-administration';

/** Attend la fin du court délai appliqué avant chaque rechargement de la liste. */
const attendreRechargement = () => new Promise((fin) => setTimeout(fin, 300));

describe('UtilisateursAdministration', () => {
  let api: HttpTestingController;

  async function afficherPage() {
    await TestBed.configureTestingModule({
      imports: [UtilisateursAdministration],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(UtilisateursAdministration);
    fixture.detectChanges();
    api.expectOne('/api/inscription/delegations').flush([]);
    await attendreRechargement();
    api.expectOne((r) => r.url === '/api/administration/utilisateurs')
      .flush({ elements: [], page: 0, taille: 20, totalElements: 0, totalPages: 0 });
    return fixture;
  }

  it('un clic sur une colonne trie par cette colonne, un second clic inverse le sens', async () => {
    const page = (await afficherPage()).componentInstance;

    page.trierPar('ROLE');
    expect(page.filtres().tri).toBe('ROLE');
    expect(page.filtres().sens).toBe('ASC');
    expect(page.indicateurTri('ROLE')).toBe('↑');

    page.trierPar('ROLE');
    expect(page.filtres().sens).toBe('DESC');
    expect(page.indicateurTri('ROLE')).toBe('↓');
  });

  it('une colonne de date se trie d’abord des plus récentes aux plus anciennes', async () => {
    const page = (await afficherPage()).componentInstance;

    page.trierPar('DERNIERE_CONNEXION');

    expect(page.filtres().sens).toBe('DESC');
  });

  it('les filtres choisis sont envoyés au serveur, et les filtres vides ne le sont pas', async () => {
    const page = (await afficherPage()).componentInstance;

    page.changerFiltres({ role: 'CHEF_EQUIPE', statut: 'ACTIF', texte: ' karim ' });
    await attendreRechargement();

    const parametres = api.expectOne((r) => r.url === '/api/administration/utilisateurs').request.params;
    expect(parametres.get('role')).toBe('CHEF_EQUIPE');
    expect(parametres.get('statut')).toBe('ACTIF');
    expect(parametres.get('texte')).toBe('karim');
    expect(parametres.has('delegation')).toBe(false);
  });

  it('effacer les filtres remet tous les filtres à vide', async () => {
    const page = (await afficherPage()).componentInstance;
    page.changerFiltres({ role: 'AGRICULTEUR', delegation: 'AGAREB' });

    page.effacerFiltres();

    expect(page.filtresActifs()).toBe(false);
  });
});
