import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import type * as L from 'leaflet';
import { Axe } from './axe.models';
import { coordonneesVille } from './villes-cemac';
import { geometrieRouteOuDroite } from './route-osrm';

const CENTRE_CEMAC: [number, number] = [6.5, 12.5];
const ZOOM_INITIAL = 5;
const POIDS_LIGNE = 3;
const POIDS_LIGNE_SELECTIONNEE = 6;

/** Alignés sur --fc-muted / --fc-primary / --fc-success (badges tableau). */
const COULEUR_VISIBILITE = '#52525b';
const COULEUR_MATCHING = '#d40f16';
const COULEUR_PAIEMENT = '#067647';

/** EF-GEO-03 : les 3 états sont indépendants — priorité d'affichage paiement > matching > visibilité. */
function couleurPourAxe(axe: Axe): string {
  if (axe.paiementActif) {
    return COULEUR_PAIEMENT;
  }
  if (axe.matchingActif) {
    return COULEUR_MATCHING;
  }
  return COULEUR_VISIBILITE;
}

function libelleEtatsAxe(axe: Axe): string {
  const etats: string[] = [];
  if (axe.visibiliteActive) etats.push('Visible sur le corridor');
  else etats.push('Masqué');
  if (axe.matchingActif) etats.push('Appariement auto');
  else etats.push('Appariement arrêté');
  if (axe.paiementActif) etats.push('Paiements ouverts');
  else etats.push('Paiements fermés');
  return etats.join(' · ');
}

/**
 * Carte géospatiale réelle (Leaflet/OpenStreetMap) du corridor CEMAC, centrée
 * sur le Cameroun et le Tchad (corridor Douala–N'Djamena). Remplace la
 * représentation schématique du Sprint 3 — voir docs/adr/0007, addendum
 * Sprint 12. Les coordonnées de hubs proviennent d'un référentiel statique
 * (villes-cemac.ts) en attendant service-geo (Moteur).
 *
 * Tracés : OSRM public (route réelle) avec fallback ligne droite (ENF-DIS-04).
 */
@Component({
  selector: 'app-corridor-map',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './corridor-map.component.html',
  styleUrl: './corridor-map.component.css',
})
export class CorridorMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() axes: Axe[] = [];
  @Input() axeSelectionneId: string | null = null;
  @Output() readonly axeSelectionne = new EventEmitter<string>();

  @ViewChild('mapHost', { static: true }) private readonly mapHost!: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private layers: L.LayerGroup | null = null;
  private viewInitialized = false;
  private readonly lignesParAxeId = new Map<string, L.Polyline>();

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    void this.initialiserCarte();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['axes'] && this.viewInitialized) {
      void this.dessinerCouches();
    } else if (changes['axeSelectionneId'] && !changes['axeSelectionneId'].firstChange) {
      this.appliquerSelection();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
    this.layers = null;
  }

  private async initialiserCarte(): Promise<void> {
    const LeafletModule: any = await import('leaflet');
    const L: any = LeafletModule.default ?? LeafletModule;

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
      void this.dessinerCouches();
    });
  }

  private async dessinerCouches(): Promise<void> {
    if (!this.map || !this.layers) {
      return;
    }
    const LeafletModule: any = await import('leaflet');
    const L: any = LeafletModule.default ?? LeafletModule;

    this.layers.clearLayers();
    this.lignesParAxeId.clear();

    const hubsVus = new Set<string>();
    const bounds: L.LatLngExpression[] = [];

    const axesAvecCoords = this.axes
      .map((axe) => {
        const origine = coordonneesVille(axe.origine);
        const destination = coordonneesVille(axe.destination);
        return origine && destination ? { axe, origine, destination } : null;
      })
      .filter((item): item is { axe: Axe; origine: [number, number]; destination: [number, number] } => item !== null);

    const geometries = await Promise.all(
      axesAvecCoords.map(({ origine, destination }) => geometrieRouteOuDroite(origine, destination))
    );

    axesAvecCoords.forEach(({ axe, origine, destination }, index) => {
      const couleur = couleurPourAxe(axe);
      const coords = geometries[index];
      const ligne = L.polyline(coords, {
        color: couleur,
        weight: POIDS_LIGNE,
        opacity: 0.9,
        lineCap: 'round',
      });
      ligne.bindPopup(
        `<strong>${this.echapper(axe.origine)} → ${this.echapper(axe.destination)}</strong><br/>` +
          `${axe.distanceKm} km<br/>${this.echapper(libelleEtatsAxe(axe))}`
      );
      ligne.on('click', () => this.axeSelectionne.emit(axe.id));
      this.layers!.addLayer(ligne);
      this.lignesParAxeId.set(axe.id, ligne);
      bounds.push(origine, destination);

      for (const [nom, hubCoords] of [
        [axe.origine, origine],
        [axe.destination, destination],
      ] as const) {
        if (hubsVus.has(nom)) {
          continue;
        }
        hubsVus.add(nom);
        const marqueur = L.circleMarker(hubCoords, {
          radius: 7,
          color: '#0a0a0a',
          weight: 1.5,
          fillColor: '#ffffff',
          fillOpacity: 1,
        });
        marqueur.bindPopup(`<strong>${this.echapper(nom)}</strong>`);
        this.layers!.addLayer(marqueur);
      }
    });

    if (bounds.length > 0 && this.map) {
      this.map.fitBounds(L.latLngBounds(bounds), { padding: [36, 36], maxZoom: 8, animate: false });
    } else {
      this.map?.setView(CENTRE_CEMAC, ZOOM_INITIAL);
    }

    requestAnimationFrame(() => this.map?.invalidateSize());
    this.appliquerSelection();
  }

  private appliquerSelection(): void {
    for (const [id, ligne] of this.lignesParAxeId) {
      const selectionnee = id === this.axeSelectionneId;
      ligne.setStyle({ weight: selectionnee ? POIDS_LIGNE_SELECTIONNEE : POIDS_LIGNE, opacity: selectionnee ? 1 : 0.9 });
      if (selectionnee) {
        ligne.bringToFront();
      }
    }
  }

  private echapper(valeur: string): string {
    return valeur
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;');
  }
}
