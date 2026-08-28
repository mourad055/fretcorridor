import { geometrieRouteOsrm, geometrieRouteOuDroite, viderCacheRouteOsrm } from './route-osrm';

describe('route-osrm', () => {
  const origine: [number, number] = [4.0511, 9.7679];
  const destination: [number, number] = [3.848, 11.5021];
  const fetchOriginal = globalThis.fetch;

  beforeEach(() => {
    viderCacheRouteOsrm();
  });

  afterEach(() => {
    globalThis.fetch = fetchOriginal;
  });

  it('decode une geometrie GeoJSON OSRM en [lat,lng]', async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'Ok',
        routes: [{ geometry: { coordinates: [[9.76, 4.05], [10.5, 3.9], [11.5, 3.84]] } }],
      }),
    }) as unknown as typeof fetch;

    const coords = await geometrieRouteOsrm(origine, destination);
    expect(coords).toEqual([
      [4.05, 9.76],
      [3.9, 10.5],
      [3.84, 11.5],
    ]);
  });

  it('retourne null puis fallback droite si OSRM echoue', async () => {
    globalThis.fetch = jest.fn().mockRejectedValue(new Error('network')) as unknown as typeof fetch;

    expect(await geometrieRouteOsrm(origine, destination)).toBeNull();
    expect(await geometrieRouteOuDroite(origine, destination)).toEqual([origine, destination]);
  });

  it('reutilise le cache memoire sans second fetch', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'Ok',
        routes: [{ geometry: { coordinates: [[9.76, 4.05], [11.5, 3.84]] } }],
      }),
    });
    globalThis.fetch = fetchMock as unknown as typeof fetch;

    await geometrieRouteOsrm(origine, destination);
    await geometrieRouteOsrm(origine, destination);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
