import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { LangueSwitchComponent } from './langue-switch.component';
import { provideTranslateServiceForTests } from '../../../../testing/translate-testing.providers';

describe('LangueSwitchComponent', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [LangueSwitchComponent],
      providers: [provideTranslateServiceForTests()],
    }).compileComponents();
  });

  it('affiche le français comme actif par défaut', () => {
    const fixture = TestBed.createComponent(LangueSwitchComponent);
    fixture.detectChanges();

    const [boutonFr, boutonEn] = fixture.debugElement.queryAll(By.css('.langue-switch__btn'));
    expect(boutonFr.nativeElement.classList).toContain('langue-switch__btn--active');
    expect(boutonEn.nativeElement.classList).not.toContain('langue-switch__btn--active');
  });

  it("bascule vers l'anglais au clic et met à jour l'état actif", () => {
    const fixture = TestBed.createComponent(LangueSwitchComponent);
    fixture.detectChanges();

    const [boutonFr, boutonEn] = fixture.debugElement.queryAll(By.css('.langue-switch__btn'));
    boutonEn.nativeElement.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.langueActuelle()).toBe('en');
    expect(boutonEn.nativeElement.classList).toContain('langue-switch__btn--active');
    expect(boutonFr.nativeElement.classList).not.toContain('langue-switch__btn--active');
  });
});
