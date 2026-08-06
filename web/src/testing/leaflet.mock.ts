/**
 * Double léger de Leaflet pour les tests Jest : jsdom n'implémente pas assez
 * du rendu SVG/Canvas pour que la vraie bibliothèque fonctionne (cf. Sprint
 * 12). Couvre uniquement l'API utilisée par CorridorMapComponent.
 */
class FakeLayer {
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
  on(): this {
    return this;
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

export function polyline(): FakeLayer {
  return new FakeLayer();
}

export function circleMarker(): FakeLayer {
  return new FakeLayer();
}

export function latLngBounds(): unknown {
  return {};
}
