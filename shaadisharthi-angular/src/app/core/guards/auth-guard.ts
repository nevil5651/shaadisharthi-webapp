import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth';

// Prevents access to routes if user is not logged in
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    return this.authService.isInitialized$.pipe(
      filter(isInitialized => isInitialized),  // Wait until AuthService is ready
      take(1),  // Only take first emission
      map(() => {
        if (this.authService.isAuthenticated()) {
          return true;  // Allow access
        }
        // Not logged in → redirect to login with return URL
        return this.router.createUrlTree(['/login'], { 
          queryParams: { returnUrl: state.url } 
        });
      })
    );
  }
}