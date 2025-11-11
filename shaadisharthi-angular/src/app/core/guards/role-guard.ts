import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth';
import { Observable } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';

// Ensures user has correct ROLE and STATUS to access a route
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}
  
  canActivate(route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> {
    return this.authService.isInitialized$.pipe(
      filter(isInitialized => isInitialized),
      take(1),
      map(() => {
        const expectedRoles = route.data['roles'] as string[] | undefined;
        const expectedStatus = route.data['status'] as string | undefined;
        
        const userRole = this.authService.getUserRole();
        const userStatus = this.authService.getUserStatus();
        
        // Check ROLE
        if (expectedRoles && (!userRole || !expectedRoles.includes(userRole))) {
          return this.router.createUrlTree(['/login']);
        }
        
        // Check STATUS (e.g., APPROVED, PENDING_APPROVAL)
        if (expectedStatus && userStatus !== expectedStatus) {
          return this.router.createUrlTree(['/login']);
        }
        
        return true;  // All checks passed
      })
    );
  }
}