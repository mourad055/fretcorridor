import { TestBed } from '@angular/core/testing';
import { LangueService } from './langue.service';
import { CLE_STOCKAGE_LANGUE } from './i18n.constants';
import { provideTranslateServiceForTests } from '../../../testing/translate-testing.providers';

describe('LangueService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTranslateServiceForTests()] });
  });

  it('utilise le français par défaut sans préférence stockée', () => {
    const service = TestBed.inject(LangueService);
    service.initialiser();

    expect(service.langueActuelle()).toBe('fr');
  });

  it('reprend la langue stockée en localStorage à l\'initialisation', () => {
    localStorage.setItem(CLE_STOCKAGE_LANGUE, 'en');
    const service = TestBed.inject(LangueService);
    service.initialiser();

    expect(service.langueActuelle()).toBe('en');
  });

  it('ignore une valeur stockée invalide et retombe sur le français', () => {
    localStorage.setItem(CLE_STOCKAGE_LANGUE, 'de');
    const service = TestBed.inject(LangueService);
    service.initialiser();

    expect(service.langueActuelle()).toBe('fr');
  });

  it('persiste la langue choisie en localStorage', () => {
    const service = TestBed.inject(LangueService);
    service.definir('en');

    expect(localStorage.getItem(CLE_STOCKAGE_LANGUE)).toBe('en');
    expect(service.langueActuelle()).toBe('en');
  });
});
