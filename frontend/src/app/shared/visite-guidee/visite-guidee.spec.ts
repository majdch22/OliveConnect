import { TestBed } from '@angular/core/testing';

import { EtapeVisite, VisiteGuidee } from './visite-guidee';

describe('VisiteGuidee', () => {
  const etapes: EtapeVisite[] = [
    { cible: 'premier', titre: 'Premier', texte: 'Un' },
    { cible: 'second', titre: 'Second', texte: 'Deux' },
  ];

  function afficher() {
    TestBed.configureTestingModule({ imports: [VisiteGuidee] });
    const fixture = TestBed.createComponent(VisiteGuidee);
    fixture.componentRef.setInput('etapes', etapes);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('avance d’étape en étape puis signale la fin au dernier « Suivant »', () => {
    const visite = afficher();
    let terminee = false;
    visite.termine.subscribe(() => (terminee = true));

    visite.suivant();
    expect(visite.etape().titre).toBe('Second');
    expect(terminee).toBe(false);

    visite.suivant();
    expect(terminee).toBe(true);
  });

  it('la touche Échap quitte la visite', () => {
    const visite = afficher();
    let terminee = false;
    visite.termine.subscribe(() => (terminee = true));

    visite.clavier(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(terminee).toBe(true);
  });
});
