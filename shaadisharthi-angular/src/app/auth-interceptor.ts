import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './core/services/auth';
import { environment } from '../environments/environment';

// Automatically adds JWT token to every API request
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const authToken = this.authService.getToken();

    // Only add token to requests going to our backend (not external like Cloudinary)
    if (authToken && request.url.startsWith(environment.apiUrl)) {
      const authReq = request.clone({
        headers: request.headers.set('Authorization', `Bearer ${authToken}`),
      });

      return next.handle(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
          // If backend returns 401 → token expired or invalid
          if (error.status === 401) {
            this.authService.logout();  // Force logout globally
          }
          return throwError(() => error);  // Let component handle error
        })
      );
    }

    // For non-API requests (e.g. images), send as-is
    return next.handle(request);
  }
}