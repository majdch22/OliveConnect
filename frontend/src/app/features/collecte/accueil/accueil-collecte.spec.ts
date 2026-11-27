import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AccueilCollecte } from './accueil-collecte';

describe('AccueilCollecte', () => {
  function accueil(): AccueilCollecte {
    TestBed.configureTestingModule({
      imports: [AccueilCollecte],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    return TestBed.createComponent(AccueilCollecte).componentInstance;
  }

  it('écrit le délai moyen de prise en charge en toutes lettres', () => {
    const page = accueil();
    expect(page.delai(null)).toBe('—');
    expect(page.delai(0)).toBe("moins d'une minute");
    expect(page.delai(12)).toBe('12 min');
    expect(page.delai(85)).toBe('1 h 25');
    expect(page.delai(60 * 72)).toBe('3 jours');
  });

  it('borne la longueur d’une barre entre 0 et 100 %', () => {
    const page = accueil();
    expect(page.pourcentage(3500, 5000)).toBe(70);
    expect(page.pourcentage(7000, 5000)).toBe(100);
    expect(page.pourcentage(10, 0)).toBe(0);
  });
});
