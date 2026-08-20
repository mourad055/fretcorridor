import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../core/auth/auth.service';
import { Session } from '../../core/auth/auth.models';
import { provideTranslateServiceForTests } from '../../../testing/translate-testing.providers';

describe('ShellComponent', () => {
  let logoutSpy: jest.Mock;
  let session: Session | null;

  function configure(): void {
    session = { token: 't', role: 'BUREAU', tenantId: 'tenant-bgft-douala', actorId: 'actor-1' };
    logoutSpy = jest.fn();
    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        provideTranslateServiceForTests(),
        {
          provide: AuthService,
          useValue: { session: () => session, logout: logoutSpy },
        },
      ],
    });
  }

  it("affiche le libellé du rôle et l'identifiant du tenant", () => {
    configure();
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Bureau de fret');
    expect(fixture.nativeElement.textContent).toContain('tenant-bgft-douala');
  });

  it('déconnecte et redirige vers /login au clic', () => {
    configure();
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigateByUrl');

    fixture.nativeElement.querySelector('.shell__actions > button').click();

    expect(logoutSpy).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith('/login');
  });
});
