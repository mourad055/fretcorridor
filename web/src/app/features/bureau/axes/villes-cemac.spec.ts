import { coordonneesVille } from './villes-cemac';

describe('coordonneesVille', () => {
  it('retourne les coordonnées connues des villes du corridor', () => {
    expect(coordonneesVille('Douala')).toEqual([4.0511, 9.7679]);
    expect(coordonneesVille("N'Djamena")).toEqual([12.1348, 15.0557]);
  });

  it('retourne null pour une ville inconnue', () => {
    expect(coordonneesVille('Ville Inconnue')).toBeNull();
  });
});
