import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CompteAdministration, PageResultat } from '../administration.modeles';
import { InscriptionsAdministration } from './inscriptions-administration';

const NADIA: CompteAdministration = {
  id: 'compte-nadia',
  nom: 'Kammoun',
  prenom: 'Nadia',
  email: null,
  telephone: '+21655000210',
  role: 'AGRICULTEUR',
  libelleRole: 'Agriculteur',
  statut: 'EN_ATTENTE_VALIDATION',
  motDePasseProvisoire: false,
  dateCreation: '2026-11-24T08:00:00Z',
  derniereConnexion: null,
  cin: '09876543',
  delegation: 'AGAREB',
  libelleDelegation: 'Agareb',
  numeroAdherent: null,
  dateTraitementInscription: null,
  motifRefus: null,
  pressoirId: null,
};

function page(elements: CompteAdministration[]): PageResultat<CompteAdministration> {
  return { elements, page: 0, taille: 10, totalElements: elements.length, totalPages: 1 };
}

/** Attend la fin du court délai appliqué avant chaque rechargement de la liste. */
const attendreRechargement = () => new Promise((fin) => setTimeout(fin, 300));

describe('InscriptionsAdministration', () => {
  let api: HttpTestingController;

  async function afficherPage() {
    await TestBed.configureTestingModule({
      imports: [InscriptionsAdministration],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    api = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(InscriptionsAdministration);
    fixture.detectChanges();
    api.expectOne('/api/inscription/delegations').flush([{ code: 'AGAREB', libelle: 'Agareb' }]);
    await attendreRechargement();
    return fixture;
  }

  it('demande par défaut les inscriptions en attente, les plus récentes d’abord', async () => {
    await afficherPage();

    const requete = api.expectOne((r) => r.url === '/api/administration/inscriptions');
    expect(requete.request.params.get('etat')).toBe('EN_ATTENTE');
    expect(requete.request.params.get('tri')).toBe('DATE_CREATION');
    expect(requete.request.params.get('sens')).toBe('DESC');
    requete.flush(page([NADIA]));
  });

  it('envoie l’onglet, la délégation et le tri choisis, et revient à la première page', async () => {
    const fixture = await afficherPage();
    api.expectOne((r) => r.url === '/api/administration/inscriptions').flush(page([NADIA]));

    fixture.componentInstance.changerFiltres({ page: 2 });
    fixture.componentInstance.changerFiltres({ etat: 'REFUSEE', delegation: 'AGAREB' });
    fixture.componentInstance.changerOrdre('nom-croissant');
    await attendreRechargement();

    const parametres = api.expectOne((r) => r.url === '/api/administration/inscriptions').request.params;
    expect(parametres.get('etat')).toBe('REFUSEE');
    expect(parametres.get('delegation')).toBe('AGAREB');
    expect(parametres.get('tri')).toBe('NOM');
    expect(parametres.get('sens')).toBe('ASC');
    expect(parametres.get('page')).toBe('0');
  });

  it('accepter une inscription affiche le numéro d’adhérent attribué', async () => {
    const fixture = await afficherPage();
    api.expectOne((r) => r.url === '/api/administration/inscriptions').flush(page([NADIA]));

    fixture.componentInstance.accepter(NADIA);
    api.expectOne('/api/administration/inscriptions/compte-nadia/accepter')
      .flush({ ...NADIA, statut: 'ACTIF', numeroAdherent: 'ADH-0047' });

    expect(fixture.componentInstance.messageSucces()).toContain('ADH-0047');
    expect(fixture.componentInstance.selection()).toBeNull();
  });

  it('un refus sans motif n’est pas envoyé', async () => {
    const fixture = await afficherPage();
    api.expectOne((r) => r.url === '/api/administration/inscriptions').flush(page([NADIA]));

    fixture.componentInstance.confirmerRefus(NADIA);

    api.expectNone('/api/administration/inscriptions/compte-nadia/refuser');
    expect(fixture.componentInstance.motif.touched).toBe(true);
  });
});
