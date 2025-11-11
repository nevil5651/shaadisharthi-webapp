// auth.ts
import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, switchMap, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { LoginApiResponse, User, UserProfile, UserStatus } from '../models/auth.model';
import { ToastrService } from 'ngx-toastr';
import { ApiService } from './api';

/** Keys used for localStorage – defined in environment file */
const { tokenStorageKey, userStorageKey } = environment.auth;

/**
 * Central authentication service.
 * Handles login, token storage, user profile, logout, and navigation based on user status.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  /** Emits the current logged-in user (or null) */
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$: Observable<User | null> = this.currentUserSubject.asObservable();

  /** Signals when the service has finished checking stored credentials on app start */
  private isInitialized = new BehaviorSubject<boolean>(false);
  public isInitialized$ = this.isInitialized.asObservable();

  constructor(
    private apiService: ApiService,   // wrapper around HttpClient
    private router: Router,
    private toastr: ToastrService
  ) {
    // Defer initialization to the next macrotask.
    // This avoids a circular dependency when AuthInterceptor injects AuthService.
    setTimeout(() => this.initializeAuthState(), 0);
  }

  /** Load token & user from localStorage and refresh profile if needed */
  private initializeAuthState(): void {
    const token = this.getToken();
    const storedUser = localStorage.getItem(userStorageKey);

    // No stored session → nothing to do
    if (!token || !storedUser) {
      this.isInitialized.next(true);
      return;
    }

    try {
      this._setCurrentUser(JSON.parse(storedUser) as User);

      // Refresh profile to validate token & get latest data.
      // `finalize` guarantees isInitialized is emitted even if the call fails.
      this.fetchUserProfile().pipe(
        finalize(() => this.isInitialized.next(true))
      ).subscribe({
        error: (err) => {
          console.warn('AuthService: Could not refresh user profile on reload.', err);
          // 401/403 will be handled globally by AuthInterceptor → logout()
        }
      });
    } catch (e) {
      console.error('AuthService: Error parsing stored user data. Logging out.', e);
      this.logout(false);          // clear corrupted data, no redirect
      this.isInitialized.next(true);
    }
  }

  // -----------------------------------------------------------------
  // Login
  // -----------------------------------------------------------------
  /** Perform login → store token → fetch profile (if approved) → navigate */
  login(email: string, password: string): Observable<User> {
    return this.apiService.post<LoginApiResponse>('ServiceProvider/login', { email, password })
      .pipe(
        // Normalise API response: snake_case → camelCase
        map(response => {
          const { provider_id, ...rest } = response;
          return { ...rest, providerId: provider_id };
        }),
        switchMap((user: User) => {
          if (!user?.token) {
            return throwError(() => new Error('Invalid login response'));
          }

          this.setToken(user.token);
          this._setCurrentUser(user);   // temporary user object

          // APPROVED users need full profile before navigating
          if (user.status === 'APPROVED') {
            return this.fetchUserProfile().pipe(
              tap(() => this.navigateUserByStatus(user.status))
            );
          } else {
            this.navigateUserByStatus(user.status);
            return of(this.currentUserSubject.value!);
          }
        })
      );
  }

  // -----------------------------------------------------------------
  // Profile handling
  // -----------------------------------------------------------------
  /** Pull the latest user profile from the backend */
  fetchUserProfile(): Observable<User> {
    return this.apiService.get<UserProfile & { provider_id?: string }>('ServiceProvider/account', {
      params: { token: this.getToken() || '' }
    }).pipe(
      map((userProfile) => {
        const currentUser = this.currentUserSubject.value;
        if (!currentUser) {
          throw new Error('Cannot fetch profile without a current user session.');
        }

        const { provider_id, ...profileData } = userProfile;
        const updatedUser: User = { ...currentUser, ...profileData };
        if (provider_id) {
          updatedUser.providerId = provider_id;
        }

        this._setCurrentUser(updatedUser);
        return updatedUser;
      })
    );
  }

  /** Update parts of the profile and refresh the full object */
  updateProfile(profileData: Partial<UserProfile>): Observable<User> {
    return this.apiService.post<void>('ServiceProvider/account', profileData).pipe(
      switchMap(() => this.fetchUserProfile())
    );
  }

  /** Change password endpoint */
  changePassword(passwordData: any): Observable<unknown> {
    return this.apiService.post('ServiceProvider/provider-change-password', passwordData);
  }

  // -----------------------------------------------------------------
  // Simple getters
  // -----------------------------------------------------------------
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getUserRole(): string | null {
    const user = this.currentUserSubject.value;
    return user ? user.role : null;
  }

  getUserStatus(): UserStatus | null {
    const user = this.currentUserSubject.value;
    return user ? user.status : null;
  }

  getToken(): string | null {
    return localStorage.getItem(tokenStorageKey);
  }

  private setToken(token: string): void {
    localStorage.setItem(tokenStorageKey, token);
  }

  getProviderId(): string | null {
    const user = this.currentUserSubject.value;
    return user ? user.providerId : null;
  }

  // -----------------------------------------------------------------
  // Internal helpers for user state
  // -----------------------------------------------------------------
  /** Persist user object to BehaviorSubject + localStorage */
  private _setCurrentUser(user: User | null): void {
    this.currentUserSubject.next(user);
    localStorage.setItem(userStorageKey, JSON.stringify(user || null));
  }

  /** Remove everything related to the session */
  private _clearAuthData(): void {
    localStorage.removeItem(tokenStorageKey);
    localStorage.removeItem(userStorageKey);
    this.currentUserSubject.next(null);
  }

  // -----------------------------------------------------------------
  // Logout
  // -----------------------------------------------------------------
  logout(redirectToLogin: boolean = true): void {
    this._clearAuthData();
    if (redirectToLogin) {
      this.toastr.success('You have been logged out successfully.');
      this.router.navigate(['/login']);
    }
  }

  /** Logout that also calls the backend (used from header UI) */
  logoutFromServer(): void {
    this.apiService.post<void>('ServiceProvider/logout', {}).pipe(
      catchError(err => {
        console.error('Server logout failed. Proceeding with local logout.', err);
        return of(null);
      }),
      finalize(() => this.logout())   // always do local logout + redirect
    ).subscribe();
  }

  // -----------------------------------------------------------------
  // Miscellaneous auth flows
  // -----------------------------------------------------------------
  requestVerificationEmail(email: string): Observable<unknown> {
    return this.apiService.post<unknown>('ServiceProvider/verify-email', { email });
  }

  verifyEmailToken(token: string): Observable<{ email: string }> {
    return this.apiService.get<{ email: string }>('ServiceProvider/email-verification', { params: { token } });
  }

  register(data: { name: string; email: string; password: string }): Observable<unknown> {
    return this.apiService.post<unknown>('ServiceProvider/register', data);
  }

  submitBusinessDetails(data: any): Observable<unknown> {
    return this.apiService.post<unknown>('ServiceProvider/business-register', data);
  }

  // -----------------------------------------------------------------
  // Navigation based on user status after login
  // -----------------------------------------------------------------
  private navigateUserByStatus(status: UserStatus): void {
    switch (status) {
      case 'BASIC_REGISTERED':
        this.router.navigate(['/business-details']);
        break;
      case 'PENDING_APPROVAL':
        this.router.navigate(['/waiting-approval']);
        break;
      case 'APPROVED':
        this.router.navigate(['/dashboard']);
        break;
      default:
        this.router.navigate(['/login']);
    }
  }
}