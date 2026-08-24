import { nombreDePages, paginer } from './pagination';

describe('paginer', () => {
  const items = Array.from({ length: 25 }, (_, i) => i + 1);

  it('retourne la premiere page', () => {
    expect(paginer(items, 1, 10)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
  });

  it('retourne la derniere page partielle', () => {
    expect(paginer(items, 3, 10)).toEqual([21, 22, 23, 24, 25]);
  });

  it('retourne un tableau vide au-dela de la derniere page', () => {
    expect(paginer(items, 4, 10)).toEqual([]);
  });
});

describe('nombreDePages', () => {
  it('arrondit au superieur', () => {
    expect(nombreDePages(25, 10)).toBe(3);
    expect(nombreDePages(20, 10)).toBe(2);
  });

  it('renvoie au moins 1 page meme sur une liste vide', () => {
    expect(nombreDePages(0, 10)).toBe(1);
  });
});
