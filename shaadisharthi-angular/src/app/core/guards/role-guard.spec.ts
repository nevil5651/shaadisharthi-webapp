import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { RoleGuard } from './role-guard';

describe('roleGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => new RoleGuard(null as any, null as any).canActivate(guardParameters[0]));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});