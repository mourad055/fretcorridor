import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import type * as L from 'leaflet';
import { Axe } from './axe.models';
import { coordonneesVille } from './villes-cemac';

const CENTRE_CEMAC: [number, number] = [6.5, 12.5];
const ZOOM_INITIAL = 5;

const COULEUR_PAR_ETAT: Record<string, string> = {
  VISIBILITE: '#a1a1aa',
  MATCHING: '#d40f16',
  PAIEMENT: '#067647',
};

/**
 * Carte géospatiale réelle (Leaflet/OpenStreetMap) du corridor CEMAC, centrée
 * sur le Cameroun et le Tchad (corridor Douala–N'Djamena). Remplace la
 * représentation schématique du Sprint 3 — voir docs/adr/0007, addendum
 * Sprint 12. Les coordonnées de hubs proviennent d'un référentiel statique
 * (villes-cemac.ts) en attendant service-geo (Moteur).
 */
@Component({
  selector: 'app-corridor-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './corridor-map.component.html',
  styleUrl: './corridor-map.component.css',
})
export class CorridorMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() axes: Axe[] = [];

  @ViewChild('mapHost', { static: true }) private readonly mapHost!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private layers: L.LayerGroup | null = null;
  private viewInitialized = false;

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    void this.initialiserCarte();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['axes'] && this.viewInitialized) {
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

    // Le conteneur peut ne pas avoir sa taille finale au moment de l'init
    // (panneau qui vient d'apparaître) : recalculer la taille AVANT de
    // dessiner les couches, sinon fitBounds se base sur une origine de
    // pixels obsolète et désaligne la grille de tuiles.
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

    const hubsVus = new Set<string>();
    const bounds: L.LatLngExpression[] = [];

    for (const axe of this.axes) {
      const origine = coordonneesVille(axe.origine);
      const destination = coordonneesVille(axe.destination);
      if (!origine || !destination) {
        continue;
      }

      const couleur = COULEUR_PAR_ETAT[axe.etatActivation] ?? COULEUR_PAR_ETAT['VISIBILITE'];
      const ligne = L.polyline([origine, destination], {
        color: couleur,
        weight: 3,
        opacity: 0.9,
        lineCap: 'round',
      });
      ligne.bindPopup(
        `<strong>${this.echapper(axe.origine)} → ${this.echapper(axe.destination)}</strong><br/>` +
          `${axe.distanceKm} km — ${this.echapper(axe.etatActivation)}`
      );
      this.layers.addLayer(ligne);
      bounds.push(origine, destination);

      for (const [nom, coords] of [
        [axe.origine, origine],
        [axe.destination, destination],
      ] as const) {
        if (hubsVus.has(nom)) {
          continue;
        }
        hubsVus.add(nom);
        const marqueur = L.circleMarker(coords, {
          radius: 7,
          color: '#0a0a0a',
          weight: 1.5,
          fillColor: '#ffffff',
          fillOpacity: 1,
        });
        marqueur.bindPopup(`<strong>${this.echapper(nom)}</strong>`);
        this.layers.addLayer(marqueur);
      }
    }

    if (bounds.length > 0 && this.map) {
      // animate: false — sinon les tuiles du zoom initial (5) restent
      // visibles sous celles du zoom final pendant la transition animée,
      // produisant un damier incohérent (cf. docs/adr/0007, addendum Sprint 12).
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [36, 36], maxZoom: 8, animate: false });
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
