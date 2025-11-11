import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ToastrModule } from 'ngx-toastr';

import { EmailVerificationComponent } from './email-verification';

describe('EmailVerification', () => {
  let component: EmailVerificationComponent;
  let fixture: ComponentFixture<EmailVerificationComponent>;

  // Setup test bed with mocks
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        EmailVerificationComponent,
        HttpClientTestingModule,   // Mock HTTP (for AuthService)
        RouterTestingModule,       // Mock navigation
        ToastrModule.forRoot(),    // Mock toasts
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EmailVerificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Basic test: component loads
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});