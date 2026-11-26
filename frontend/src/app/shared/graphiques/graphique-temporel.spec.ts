import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { GraphiqueTemporel, PointTemporel } from './graphique-temporel';

function points(valeurs: (number | null)[]): PointTemporel[] {
  return valeurs.map((valeur, indice) => ({ moment: new Date(Date.UTC(2026, 10, 25, 10, indice)).toISOString(), valeur }));
}

describe('GraphiqueTemporel', () => {
  async function afficher(valeurs: (number | null)[], maximumFixe: number | null = null) {
    // Mêmes réglages régionaux que l'application (virgule décimale)
    registerLocaleData(localeFr);
    await TestBed.configureTestingModule({
      imports: [GraphiqueTemporel],
      providers: [{ provide: LOCALE_ID, useValue: 'fr' }],
    }).compileComponents();
    const fixture = TestBed.createComponent(GraphiqueTemporel);
    fixture.componentRef.setInput('titre', 'Processeur');
    fixture.componentRef.setInput('unite', '%');
    fixture.componentRef.setInput('points', points(valeurs));
    fixture.componentRef.setInput('maximumFixe', maximumFixe);
    await fixture.whenStable();
    return fixture;
  }

  it('affiche un message tant qu’il y a moins de deux mesures', async () => {
    const fixture = await afficher([12]);

    expect(fixture.nativeElement.textContent).toContain('Mesures en cours de collecte');
  });

  it('interrompt la courbe là où une valeur manque', async () => {
    const graphique = (await afficher([10, 20, null, 30, 40])).componentInstance;

    // Deux morceaux de courbe : chacun commence par « M »
    expect(graphique.trace().match(/M/g)?.length).toBe(2);
  });

  it('respecte le maximum fixe de l’axe (100 pour un pourcentage)', async () => {
    const graphique = (await afficher([10, 20], 100)).componentInstance;

    expect(graphique.maximum()).toBe(100);
  });

  it('les flèches du clavier parcourent les mesures', async () => {
    const graphique = (await afficher([10, 20, 30])).componentInstance;

    graphique.naviguerAuClavier(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    expect(graphique.survol()?.point.valeur).toBe(20);

    graphique.naviguerAuClavier(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    expect(graphique.survol()?.point.valeur).toBe(10);
  });

  it('montre la dernière valeur dans le titre', async () => {
    const fixture = await afficher([10, 20, 35.5]);

    expect(fixture.nativeElement.textContent).toContain('35,5');
  });
});
