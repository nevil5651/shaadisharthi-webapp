// Angular core & forms
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
// Routing & common modules
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
// Custom services
import { AuthService } from '../../core/services/auth';
import { ToastrService } from 'ngx-toastr';

// This component handles provider registration after email verification via token
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-provider.html',
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;        // The main reactive form
  isSubmitting = false;           // Tracks submission state (for loading spinner)
  errorMessage: string | null = null;  // Holds server-side error messages

  // Dependency injection: FormBuilder, AuthService, Router, etc.
  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: ToastrService
  ) {
    // Build the form with controls and validators
    this.registerForm = this.fb.group(
      {
        name: ['', Validators.required],  // Required name
        email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],  // Pre-filled, readonly
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            // Regex: at least 1 lowercase, 1 uppercase, 1 digit, 1 special char
            Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/),
          ],
        ],
        confirmPassword: ['', Validators.required],
        terms: [false, Validators.requiredTrue],  // Must be checked
      },
      { validators: this.passwordMatchValidator }  // Custom cross-field validator
    );
  }

  // Runs when component initializes
  ngOnInit() {
    // Get the 'token' from URL query params (e.g., ?token=abc123)
    const token = this.route.snapshot.queryParamMap.get('token');
    
    // If no token, redirect to login (security check)
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }

    // Verify the token with backend
    this.authService.verifyEmailToken(token).subscribe({
      next: (response: { email: any; }) => {
        // On success: fill email field (it's disabled in template)
        this.registerForm.patchValue({ email: response.email });
      },
      error: () => {
        // On failure: show error, redirect to email verification page
        const errorMessage = 'Invalid or expired token. Please request a new verification email.';
        this.errorMessage = errorMessage;
        console.error('Email verification failed. Redirecting to email verification page.');
        this.toastr.error(errorMessage, 'Verification Failed');
        this.router.navigate(['/email-verification']);
      },
    });
  }

  // Custom validator: checks if password and confirmPassword match
  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  // Getter methods for easy access in template (e.g., name?.invalid)
  get name() {
    return this.registerForm.get('name');
  }

  get email() {
    return this.registerForm.get('email');
  }

  get password() {
    return this.registerForm.get('password');
  }

  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }

  get terms() {
    return this.registerForm.get('terms');
  }

  // Form submission handler
  onSubmit() {
    if (this.registerForm.valid) {
      this.isSubmitting = true;        // Show loading state
      this.errorMessage = null;        // Clear previous errors

      // Extract values (email is enabled temporarily to get its value)
      const { name, email, password } = this.registerForm.getRawValue();

      // Call AuthService to register user
      this.authService.register({ name, email, password }).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.toastr.success('Registration successful! Please log in to continue.', 'Success');
          // Optional: store email for auto-fill on login
          localStorage.setItem('userEmail', email);
          this.router.navigate(['/login']);
        },
        error: (err: { error: { error: string; }; }) => {
          this.isSubmitting = false;
          // Show backend error or fallback message
          const errorMessage = err.error?.error || 'Registration failed. Please try again.';
          this.errorMessage = errorMessage;
          this.toastr.error(errorMessage, 'Registration Failed');
        },
      });
    }
  }
}