import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, Role, Session, TenantOption } from './auth.models';
import { decodeJwtPayload } from './jwt.util';

const STORAGE_KEY = 'fretcorridor.session';

interface JwtClaims {
  sub: string;
  role: Role;
  tenantId: string;
}

/**
 * Résolution du rôle et du tenant depuis le JWT (FE-WEB-02) — jamais depuis un
 * champ transmis en clair par ailleurs. Session tenue en Signals (pas NgRx,
 * cf. PRD §4.2).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionSignal = signal<Session | null>(this.restoreSession());

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);
  readonly role = computed(() => this.sessionSignal()?.role ?? null);

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request)
      .pipe(tap((response) => this.storeSession(response)));
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.sessionSignal.set(null);
  }

  /**
   * S18 : tenant d'origine + affiliations accordées par un second bureau.
   * Le cas normal (un seul tenant) ne déclenche aucun écran de choix.
   */
  mesTenants(): Observable<TenantOption[]> {
    return this.http.get<TenantOption[]>(`${environment.apiBaseUrl}/auth/tenants`);
  }

  /**
   * Réémet un JWT scopé au tenant choisi (même forme que login) — le serveur
   * refuse tout tenant auquel l'acteur n'est pas affilié.
   */
  selectionnerTenant(tenantId: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/tenants/selection`, { tenantId })
      .pipe(tap((response) => this.storeSession(response)));
  }

  private storeSession(response: LoginResponse): void {
    const claims = decodeJwtPayload<JwtClaims>(response.token);
    const session: Session = {
      token: response.token,
      role: response.role,
      tenantId: response.tenantId,
      actorId: claims?.sub ?? '',
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.sessionSignal.set(session);
  }

  private restoreSession(): Session | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as Session;
    } catch {
      return null;
    }
  }
}
