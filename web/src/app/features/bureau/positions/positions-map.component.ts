import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import type * as L from 'leaflet';
import { Position, formatAge } from './position.models';

const CENTRE_CEMAC: [number, number] = [6.5, 12.5];
const ZOOM_INITIAL = 5;

/**
 * Carte de suivi temps réel (FE-TRK-04/RG-043, audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.5) : le PRD (Sprint 6)
 * exige explicitement une carte, pas seulement un tableau de coordonnées
 * brutes — même pattern Leaflet que CorridorMapComponent (axes), mais des
 * marqueurs ponctuels plutôt que des polylignes puisque les positions
 * portent déjà leurs coordonnées (pas de lookup par nom de ville nécessaire).
 */
@Component({
  selector: 'app-positions-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './positions-map.component.html',
  styleUrl: './positions-map.component.css',
})
export class PositionsMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() positions: Position[] = [];

  @ViewChild('mapHost', { static: true }) private readonly mapHost!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private layers: L.LayerGroup | null = null;
  private viewInitialized = false;

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    void this.initialiserCarte();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['positions'] && this.viewInitialized) {
      this.dessinerCouches();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
    this.layers = null;
  }

  private async initialiserCarte(): Promise<void> {
    const L = await import('leaflet');

    this.map = L.map(this.mapHost.nativeElement, {
      zoomControl: true,
      attributionControl: true,
    }).setView(CENTRE_CEMAC, ZOOM_INITIAL);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);

    this.layers = L.layerGroup().addTo(this.map);

    requestAnimationFrame(() => {
      this.map?.invalidateSize();
      this.dessinerCouches();
    });
  }

  private async dessinerCouches(): Promise<void> {
    if (!this.map || !this.layers) {
      return;
    }
    const L = await import('leaflet');

    this.layers.clearLayers();

    const bounds: L.LatLngExpression[] = [];

    for (const position of this.positions) {
      const coords: L.LatLngExpression = [position.latitude, position.longitude];
      const marqueur = L.circleMarker(coords, {
        radius: 8,
        color: '#0a0a0a',
        weight: 1.5,
        fillColor: '#d40f16',
        fillOpacity: 0.9,
      });
      marqueur.bindPopup(
        `<strong>${this.echapper(position.vehiculeLabel)}</strong><br/>${this.echapper(formatAge(position.ageSecondes))}`
      );
      this.layers.addLayer(marqueur);
      bounds.push(coords);
    }

    if (bounds.length > 0 && this.map) {
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [36, 36], maxZoom: 10, animate: false });
    } else {
      this.map?.setView(CENTRE_CEMAC, ZOOM_INITIAL);
    }

    requestAnimationFrame(() => this.map?.invalidateSize());
  }

  private echapper(valeur: string): string {
    return valeur
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;');
  }
}
