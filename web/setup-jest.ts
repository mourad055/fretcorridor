import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';
import { toHaveNoViolations } from 'jest-axe';

setupZoneTestEnv();

// Objective l'audit WCAG AA (DESIGN.md, PRODUCT.md) au lieu de rester
// déclaratif (audit UX 2026-08-23) : expect(...).toHaveNoViolations()
// disponible dans toute la suite Jest sans import répété par fichier.
expect.extend(toHaveNoViolations);

// jsdom n'implémente pas URL.createObjectURL/revokeObjectURL (utilisés par
// tous les exports CSV côté client) — sans ce stub, l'exception levée dans
// le subscribe() est asynchrone et non rattachée au bon test par Jest,
// parfois signalée à tort sur un test suivant sans rapport (trouvé en
// étendant la couverture jest-axe, audit UX 2026-08-23 §3.6).
if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = jest.fn(() => 'blob:mock');
}
if (typeof URL.revokeObjectURL !== 'function') {
  URL.revokeObjectURL = jest.fn();
}
