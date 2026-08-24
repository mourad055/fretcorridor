import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { PaginationComponent } from './pagination.component';

describe('PaginationComponent', () => {
  it('ne se rend pas si le total tient sur une seule page', () => {
    const fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentInstance.page = 1;
    fixture.componentInstance.total = 10;
    fixture.componentInstance.taillePage = 20;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.pagination')).toBeNull();
  });

  it('affiche la page courante et le nombre total de pages', () => {
    const fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentInstance.page = 2;
    fixture.componentInstance.total = 45;
    fixture.componentInstance.taillePage = 20;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Page 2 / 3');
  });

  it('desactive Precedent sur la premiere page et Suivant sur la derniere', () => {
    const fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentInstance.page = 1;
    fixture.componentInstance.total = 45;
    fixture.componentInstance.taillePage = 20;
    fixture.detectChanges();

    const [precedent, suivant] = fixture.debugElement.queryAll(By.css('button'));
    expect(precedent.nativeElement.disabled).toBe(true);
    expect(suivant.nativeElement.disabled).toBe(false);
  });

  it('emet pageChange au clic sur Suivant puis Precedent', () => {
    const fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentInstance.page = 2;
    fixture.componentInstance.total = 45;
    fixture.componentInstance.taillePage = 20;
    fixture.detectChanges();

    const emis: number[] = [];
    fixture.componentInstance.pageChange.subscribe((p) => emis.push(p));

    const [precedent, suivant] = fixture.debugElement.queryAll(By.css('button'));
    suivant.nativeElement.click();
    precedent.nativeElement.click();

    expect(emis).toEqual([3, 1]);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    const fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentInstance.page = 1;
    fixture.componentInstance.total = 45;
    fixture.componentInstance.taillePage = 20;
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
