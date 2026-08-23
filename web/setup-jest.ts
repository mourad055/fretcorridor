import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';
import { toHaveNoViolations } from 'jest-axe';

setupZoneTestEnv();

// Objective l'audit WCAG AA (DESIGN.md, PRODUCT.md) au lieu de rester
// déclaratif (audit UX 2026-08-23) : expect(...).toHaveNoViolations()
// disponible dans toute la suite Jest sans import répété par fichier.
expect.extend(toHaveNoViolations);
