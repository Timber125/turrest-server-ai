import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../../shared/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8081';

  private currentUser = signal<User | null>(null);
  private sessionInvalidated = signal(false);

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly isSessionInvalidated = this.sessionInvalidated.asReadonly();

  constructor(private http: HttpClient) {
    this.loadStoredUser();
  }

  private loadStoredUser(): void {
    const stored = sessionStorage.getItem('user');
    if (stored) {
      try {
        this.currentUser.set(JSON.parse(stored));
      } catch {
        sessionStorage.removeItem('user');
      }
    }
  }

  private storeUser(user: User): void {
    sessionStorage.setItem('user', JSON.stringify(user));
    this.currentUser.set(user);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap(response => {
        const user: User = {
          id: response.userid,
          username: response.username,
          token: response.accessToken
        };
        this.storeUser(user);
        this.clearSessionInvalidated();
      })
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap(response => {
        const user: User = {
          id: response.userid,
          username: response.username,
          token: response.accessToken
        };
        this.storeUser(user);
      })
    );
  }

  logout(): void {
    sessionStorage.removeItem('user');
    this.currentUser.set(null);
  }

  markSessionInvalidated(): void {
    this.sessionInvalidated.set(true);
  }

  clearSessionInvalidated(): void {
    this.sessionInvalidated.set(false);
  }

  refreshToken(): Observable<AuthResponse> {
    const userId = this.getUserId();
    if (!userId) {
      throw new Error('No user ID found for refresh');
    }
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, { userId }).pipe(
      tap(response => {
        const user: User = {
          id: response.userid,
          username: response.username,
          token: response.accessToken
        };
        this.storeUser(user);
      })
    );
  }

  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }

  getUserId(): string | null {
    return this.currentUser()?.id ?? null;
  }
}
