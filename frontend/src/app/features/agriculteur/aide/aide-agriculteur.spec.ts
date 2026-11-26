import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { AideAgriculteur } from './aide-agriculteur';

describe('AideAgriculteur', () => {
  function afficher(depuis: string | null) {
    TestBed.configureTestingModule({
      imports: [AideAgriculteur],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(depuis ? { depuis } : {}) } } },
      ],
    });
    return TestBed.createComponent(AideAgriculteur).componentInstance;
  }

  it('propose en premier l’aide de l’écran d’où l’on vient', () => {
    expect(afficher('/agriculteur/demandes/nouvelle').guideDeLaPage()?.titre).toBe('Déclarer une récolte en 1 minute');
  });

  it('la recherche ignore les accents et les majuscules', () => {
    const aide = afficher(null);

    aide.recherche.set('GPS');
    expect(aide.questions().map((entree) => entree.question)).toContain('Le GPS ne fonctionne pas');

    aide.recherche.set('DEMARRE');
    expect(aide.guides().map((guide) => guide.titre)).toContain('Modifier ou annuler une collecte');
  });
});
