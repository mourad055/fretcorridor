import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { axe } from 'jest-axe';
import { ShellSidebarComponent } from './shell-sidebar.component';
import { AuthService } from '../../core/auth/auth.service';
import { Session } from '../../core/auth/auth.models';
import { provideTranslateServiceForTests } from '../../../testing/translate-testing.providers';

describe('ShellSidebarComponent', () => {
  function configure(session: Session | null): void {
    TestBed.configureTestingModule({
      imports: [ShellSidebarComponent],
      providers: [
        provideRouter([]),
        provideTranslateServiceForTests(),
        { provide: AuthService, useValue: { session: () => session } },
      ],
    });
  }

  it('regroupe les onglets Admin par groupe (Conformité/Finance/Configuration)', () => {
    configure({ token: 't', role: 'ADMIN', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' });
    const fixture = TestBed.createComponent(ShellSidebarComponent);
    fixture.detectChanges();

    const titres = fixture.debugElement.queryAll(By.css('.shell-sidebar__groupe-titre'));
    expect(titres.map((t) => t.nativeElement.textContent.trim())).toEqual(['Conformité', 'Finance', 'Configuration']);

    const links = fixture.debugElement.queryAll(By.css('.shell-sidebar__link'));
    expect(links).toHaveLength(9);
    expect(links.map((l) => l.nativeElement.textContent.trim())).toEqual(
      expect.arrayContaining(['Comptes', 'Recherche', 'Notifications'])
    );
  });

  it("n'affiche aucun titre de groupe pour le rôle Transporteur (onglets non groupés)", () => {
    configure({ token: 't', role: 'TRANSPORTEUR', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' });
    const fixture = TestBed.createComponent(ShellSidebarComponent);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('.shell-sidebar__groupe-titre'))).toHaveLength(0);
    expect(fixture.debugElement.queryAll(By.css('.shell-sidebar__link'))).toHaveLength(3);
  });

  it("n'affiche aucun onglet sans session", () => {
    configure(null);
    const fixture = TestBed.createComponent(ShellSidebarComponent);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('.shell-sidebar__link'))).toHaveLength(0);
  });

  it("n'a aucune violation d'accessibilité automatiquement détectable", async () => {
    configure({ token: 't', role: 'ADMIN', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' });
    const fixture = TestBed.createComponent(ShellSidebarComponent);
    fixture.detectChanges();

    const resultats = await axe(fixture.nativeElement);
    expect(resultats).toHaveNoViolations();
  });
});
