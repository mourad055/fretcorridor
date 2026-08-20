import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { ActivatedRouteSnapshot } from '@angular/router';
import { roleGuard, guestGuard } from './role.guard';
import { AuthService } from './auth.service';
import { Session } from './auth.models';

function routeWithRole(role?: string): ActivatedRouteSnapshot {
  return { data: role ? { role } : {} } as unknown as ActivatedRouteSnapshot;
}

describe('roleGuard', () => {
  let authServiceStub: { session: () => Session | null };
  let router: Router;

  function configure(session: Session | null): void {
    authServiceStub = { session: () => session };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceStub }],
    });
    router = TestBed.inject(Router);
  }

  it('redirects to /login when no session exists', () => {
    configure(null);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(routeWithRole('BUREAU'), {} as never)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/login');
  });

  it('redirects to /403 when the role does not match the route', () => {
    configure({ token: 't', role: 'TRANSPORTEUR', tenantId: 'tenant-1', actorId: 'a1' });

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(routeWithRole('ADMIN'), {} as never)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/403');
  });

  it('allows navigation when the role matches the route', () => {
    configure({ token: 't', role: 'ADMIN', tenantId: 'tenant-1', actorId: 'a1' });

    const result = TestBed.runInInjectionContext(() => roleGuard(routeWithRole('ADMIN'), {} as never));

    expect(result).toBe(true);
  });
});

describe('guestGuard', () => {
  it('redirects an authenticated actor away from /login', () => {
    const authServiceStub = {
      isAuthenticated: () => true,
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceStub }],
    });
    const router = TestBed.inject(Router);

    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never)) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/');
  });

  it('allows an unauthenticated actor to reach /login', () => {
    const authServiceStub = { isAuthenticated: () => false };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceStub }],
    });

    const result = TestBed.runInInjectionContext(() => guestGuard({} as never, {} as never));

    expect(result).toBe(true);
  });
});
