import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { AideCollecte } from './aide-collecte';

describe('AideCollecte', () => {
  function afficher(depuis: string | null) {
    TestBed.configureTestingModule({
      imports: [AideCollecte],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(depuis ? { depuis } : {}) } } },
      ],
    });
    return TestBed.createComponent(AideCollecte).componentInstance;
  }

  it('propose en premier le guide de la planification quand on vient de cet écran', () => {
    expect(afficher('/collecte/planifier').guideDeLaPage()?.titre).toBe('Planifier une tournée sans conflit');
  });

  it('la recherche trouve la règle d’une ressource par jour sans tenir compte des accents', () => {
    const aide = afficher(null);
    aide.recherche.set('RESSOURCE N APPARAIT');
    expect(aide.questions().length).toBe(0);
    aide.recherche.set('n’apparait pas');
    expect(aide.questions().map((entree) => entree.question)).toContain('Pourquoi une ressource n’apparaît pas dans la planification ?');
  });
});
