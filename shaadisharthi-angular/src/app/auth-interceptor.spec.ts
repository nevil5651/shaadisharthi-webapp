import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn, HttpHandlerFn } from '@angular/common/http';

import { AuthInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next: HttpHandlerFn) =>
    TestBed.runInInjectionContext(() => new AuthInterceptor(null as any).intercept(req, next as any));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  // Basic test: interceptor function exists
  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });
});