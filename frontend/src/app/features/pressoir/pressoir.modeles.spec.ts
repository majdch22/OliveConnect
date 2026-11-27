import { categoriePour, tonnes } from './pressoir.modeles';

describe('Modèles du pressoir', () => {
  it('donne la catégorie de qualité selon les mêmes seuils que le serveur', () => {
    expect(categoriePour(0.6).code).toBe('EXTRA_VIERGE');
    expect(categoriePour(0.8).code).toBe('EXTRA_VIERGE');
    expect(categoriePour(0.81).code).toBe('VIERGE');
    expect(categoriePour(2).code).toBe('VIERGE');
    expect(categoriePour(3.3).code).toBe('COURANTE');
    expect(categoriePour(4).code).toBe('LAMPANTE');
  });

  it('écrit les grandes quantités en tonnes', () => {
    expect(tonnes(850)).toBe('850 kg');
    expect(tonnes(12400)).toBe('12,4 t');
  });
});
