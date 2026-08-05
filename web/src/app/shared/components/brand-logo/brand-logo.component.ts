import { Component, computed, input, signal } from '@angular/core';

const LOGO_CANDIDATES = ['/assets/logo.png', '/assets/logo.svg', '/assets/logo.webp'] as const;

/** Logo de marque, repris du design V3 (mêmes assets) — repli sur un libellé texte si l'image est absente. */
@Component({
  selector: 'app-brand-logo',
  standalone: true,
  template: `
    @if (logoSrc(); as src) {
      <img
        class="brand-logo"
        [class.brand-logo--sm]="size() === 'sm'"
        [class.brand-logo--md]="size() === 'md'"
        [class.brand-logo--lg]="size() === 'lg'"
        [src]="src"
        [alt]="alt()"
        (error)="onImageError()"
      />
    } @else {
      <span
        class="brand-logo__fallback"
        [class.brand-logo__fallback--sm]="size() === 'sm'"
        [class.brand-logo__fallback--md]="size() === 'md'"
        [class.brand-logo__fallback--lg]="size() === 'lg'"
      >
        FretCorridor
      </span>
    }
  `,
  styles: `
    .brand-logo {
      display: block;
      width: auto;
      max-width: 100%;
      object-fit: contain;
    }

    .brand-logo--sm {
      height: 2rem;
    }

    .brand-logo--md {
      height: 2.5rem;
    }

    .brand-logo--lg {
      height: 3.75rem;
    }

    .brand-logo__fallback {
      display: inline-block;
      font-weight: 800;
      font-style: italic;
      letter-spacing: -0.03em;
      color: var(--fc-text);
      line-height: 1;
    }

    .brand-logo__fallback--sm {
      font-size: 1rem;
    }

    .brand-logo__fallback--md {
      font-size: 1.125rem;
    }

    .brand-logo__fallback--lg {
      font-size: 1.5rem;
    }
  `,
})
export class BrandLogoComponent {
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly alt = input<string>('FretCorridor');

  private readonly candidateIndex = signal(0);

  readonly logoSrc = computed(() => {
    const index = this.candidateIndex();
    return index >= LOGO_CANDIDATES.length ? null : LOGO_CANDIDATES[index];
  });

  onImageError(): void {
    this.candidateIndex.update((value) => value + 1);
  }
}
