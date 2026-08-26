/**
 * Routage routier via OSRM public (demo) pour la carte des axes Bureau.
 * Cache mémoire + timeout court ; en cas d'échec on retombe sur la ligne
 * droite (ENF-DIS-04) — jamais bloquer l'affichage de la carte.
 */

export type LatLng = [number, number];

const OSRM_BASE = 'https://router.project-osrm.org/route/v1/driving';
const TIMEOUT_MS = 3000;

const cache = new Map<string, LatLng[]>();

function cleCache(origine: LatLng, destination: LatLng): string {
  return `${origine[0]},${origine[1]}|${destination[0]},${destination[1]}`;
}

/**
 * Retourne une polyligne [lat,lng][] suivant la route, ou null si OSRM
 * indisponible / timeout / réponse invalide.
 */
export async function geometrieRouteOsrm(origine: LatLng, destination: LatLng): Promise<LatLng[] | null> {
  const cle = cleCache(origine, destination);
  const enCache = cache.get(cle);
  if (enCache) {
    return enCache;
  }

  const url =
    `${OSRM_BASE}/${origine[1]},${origine[0]};${destination[1]},${destination[0]}` +
    `?overview=full&geometries=geojson`;

  const controleur = new AbortController();
  const timer = setTimeout(() => controleur.abort(), TIMEOUT_MS);

  try {
    const reponse = await fetch(url, { signal: controleur.signal });
    if (!reponse.ok) {
      return null;
    }
    const corps = (await reponse.json()) as {
      code?: string;
      routes?: Array<{ geometry?: { coordinates?: number[][] } }>;
    };
    if (corps.code !== 'Ok' || !corps.routes?.[0]?.geometry?.coordinates?.length) {
      return null;
    }
    // GeoJSON = [lon, lat] → Leaflet = [lat, lon]
    const coords: LatLng[] = corps.routes[0].geometry.coordinates.map((c) => [c[1], c[0]]);
    cache.set(cle, coords);
    return coords;
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

/** Route OSRM si possible, sinon segment droit origine→destination. */
export async function geometrieRouteOuDroite(origine: LatLng, destination: LatLng): Promise<LatLng[]> {
  const route = await geometrieRouteOsrm(origine, destination);
  return route ?? [origine, destination];
}

/** Réservé aux tests — vide le cache mémoire entre les cas. */
export function viderCacheRouteOsrm(): void {
  cache.clear();
}
