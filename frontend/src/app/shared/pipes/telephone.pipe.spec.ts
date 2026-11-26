import { TelephonePipe } from './telephone.pipe';

describe('TelephonePipe', () => {
  const pipe = new TelephonePipe();

  it('groupe un numéro tunisien', () => {
    expect(pipe.transform('+21655000210')).toBe('+216 55 000 210');
  });

  it('laisse tel quel un numéro dans un autre format', () => {
    expect(pipe.transform('12345')).toBe('12345');
    expect(pipe.transform(null)).toBe('');
  });
});
