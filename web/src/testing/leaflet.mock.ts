/**
 * Double léger de Leaflet pour les tests Jest : jsdom n'implémente pas assez
 * du rendu SVG/Canvas pour que la vraie bibliothèque fonctionne (cf. Sprint
 * 12). Couvre uniquement l'API utilisée par CorridorMapComponent.
 */
class FakeLayer {
  private readonly handlers = new Map<string, () => void>();

  addTo(): this {
    return this;
  }
  bindPopup(): this {
    return this;
  }
  setStyle(): this {
    return this;
  }
  bringToFront(): this {
    return this;
  }
  getBounds(): unknown {
    return {};
  }
  on(event: string, handler: () => void): this {
    this.handlers.set(event, handler);
    return this;
  }
  trigger(event: string): void {
    this.handlers.get(event)?.();
  }
}

class FakeLayerGroup extends FakeLayer {
  clearLayers(): this {
    return this;
  }
  addLayer(): this {
    return this;
  }
}

class FakeMap {
  setView(): this {
    return this;
  }
  fitBounds(): this {
    return this;
  }
  invalidateSize(): this {
    return this;
  }
  remove(): this {
    return this;
  }
}

export function map(): FakeMap {
  return new FakeMap();
}

export function tileLayer(): FakeLayer {
  return new FakeLayer();
}

export function layerGroup(): FakeLayerGroup {
  return new FakeLayerGroup();
}

/** Dernières polylignes créées, dans l'ordre — permet aux tests de déclencher leurs événements (ex. clic). */
export const polylignesCreees: FakeLayer[] = [];

export function polyline(): FakeLayer {
  const ligne = new FakeLayer();
  polylignesCreees.push(ligne);
  return ligne;
}

export function circleMarker(): FakeLayer {
  return new FakeLayer();
}

export function latLngBounds(): unknown {
  return {};
}
