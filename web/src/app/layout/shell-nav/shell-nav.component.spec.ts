import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ShellNavComponent } from './shell-nav.component';
import { AuthService } from '../../core/auth/auth.service';
import { Session } from '../../core/auth/auth.models';

describe('ShellNavComponent', () => {
  function configure(session: Session | null): void {
    TestBed.configureTestingModule({
      imports: [ShellNavComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { session: () => session } },
      ],
    });
  }

  it('affiche les onglets du rôle Bureau', () => {
    configure({ token: 't', role: 'BUREAU', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' });
    const fixture = TestBed.createComponent(ShellNavComponent);
    fixture.detectChanges();

    const links = fixture.debugElement.queryAll(By.css('a.shell-nav__link'));
    expect(links.map((l) => l.nativeElement.textContent.trim())).toEqual([
      'Axes',
      'Missions appariées',
      'Suivi temps réel',
      'Chronologie',
      'Rapport financier',
      'Notifications',
    ]);
  });

  it('affiche les onglets du rôle Transporteur', () => {
    configure({ token: 't', role: 'TRANSPORTEUR', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' });
    const fixture = TestBed.createComponent(ShellNavComponent);
    fixture.detectChanges();

    const links = fixture.debugElement.queryAll(By.css('a.shell-nav__link'));
    expect(links.map((l) => l.nativeElement.textContent.trim())).toEqual(['Capacités', 'Mes missions', 'Paiement']);
  });

  it('affiche les onglets du rôle Admin', () => {
    configure({ token: 't', role: 'ADMIN', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' });
    const fixture = TestBed.createComponent(ShellNavComponent);
    fixture.detectChanges();

    const links = fixture.debugElement.queryAll(By.css('a.shell-nav__link'));
    expect(links.map((l) => l.nativeElement.textContent.trim())).toEqual([
      'KYC',
      'Rapport financier',
      'Dossiers',
      'Configuration',
      'Tenants',
      "Journal d'audit",
    ]);
  });

  it("n'affiche aucun onglet sans session", () => {
    configure(null);
    const fixture = TestBed.createComponent(ShellNavComponent);
    fixture.detectChanges();

    expect(fixture.debugElement.queryAll(By.css('a.shell-nav__link'))).toHaveLength(0);
  });
});
