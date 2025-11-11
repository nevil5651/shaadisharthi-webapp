import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth';

// Step 1: User enters email to get verification link
@Component({
  selector: 'app-email-verification',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './email-verification.html',
})
export class EmailVerificationComponent {
  emailForm: FormGroup;           // Holds email input
  isSubmitting = false;           // Loading state
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    // Simple form: only email with required + valid email format
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  // Easy access to email control in template
  get email() {
    return this.emailForm.get('email');
  }

  // Submit: send email to backend to trigger verification email
  onSubmit() {
    if (this.emailForm.valid) {
      this.isSubmitting = true;
      this.errorMessage = null;

      const email = this.emailForm.value.email;

      this.authService.requestVerificationEmail(email).subscribe({
        next: () => {
          this.isSubmitting = false;
          // Store email temporarily so next page can show it
          localStorage.setItem('pendingEmail', email);
          // Redirect to success message page with email in URL
          this.router.navigate(['/verify-email-message'], { queryParams: { email } });
        },
        error: (err: { error: { error: string; }; }) => {
          this.isSubmitting = false;
          this.errorMessage = err.error?.error || 'Failed to send verification email. Please try again later.';
        },
      });
    }
  }
}