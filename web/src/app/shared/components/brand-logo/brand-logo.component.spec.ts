import { TestBed } from '@angular/core/testing';
import { BrandLogoComponent } from './brand-logo.component';

describe('BrandLogoComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrandLogoComponent],
    }).compileComponents();
  });

  it('affiche le logo par défaut', () => {
    const fixture = TestBed.createComponent(BrandLogoComponent);
    fixture.detectChanges();

    const img = fixture.nativeElement.querySelector('img.brand-logo');
    expect(img.src).toContain('/assets/logo.png');
  });

  it('applique la taille demandée', () => {
    const fixture = TestBed.createComponent(BrandLogoComponent);
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    const img = fixture.nativeElement.querySelector('img.brand-logo');
    expect(img.classList).toContain('brand-logo--lg');
  });

  it("bascule sur le libellé texte si toutes les images échouent", () => {
    const fixture = TestBed.createComponent(BrandLogoComponent);
    fixture.detectChanges();

    const img = fixture.nativeElement.querySelector('img.brand-logo');
    img.dispatchEvent(new Event('error'));
    img.dispatchEvent(new Event('error'));
    img.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('img.brand-logo')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('FretCorridor');
  });
});
