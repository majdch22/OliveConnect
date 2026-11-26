import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DepuisPipe } from './depuis.pipe';

describe('DepuisPipe', () => {
  const maintenant = new Date('2026-12-02T10:00:00');
  let depuis: DepuisPipe;

  beforeEach(() => {
    registerLocaleData(localeFr);
    TestBed.configureTestingModule({ providers: [DepuisPipe, { provide: LOCALE_ID, useValue: 'fr' }] });
    depuis = TestBed.inject(DepuisPipe);
  });

  it('écrit la date comme un téléphone : à l’instant, minutes, heures, hier, puis la date', () => {
    expect(depuis.transform('2026-12-02T09:59:40', maintenant)).toBe("à l'instant");
    expect(depuis.transform('2026-12-02T09:55:00', maintenant)).toBe('il y a 5 min');
    expect(depuis.transform('2026-12-02T07:00:00', maintenant)).toBe('il y a 3 h');
    expect(depuis.transform('2026-12-01T08:00:00', maintenant)).toBe('hier');
    expect(depuis.transform('2026-11-20T08:00:00', maintenant)).toBe('20/11');
  });
});
