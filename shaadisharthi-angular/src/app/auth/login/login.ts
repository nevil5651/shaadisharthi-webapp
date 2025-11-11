// login.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth';
import { ToastrService } from 'ngx-toastr';

/**
 * Login component – handles the login form, validation and submission.
 * It is a standalone component (no NgModule needed).
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,          // for *ngIf, *ngFor, etc.
    ReactiveFormsModule,   // for reactive forms
    RouterModule           // for routerLink
  ],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  /** The reactive form group that holds email, password and remember checkbox */
  loginForm: FormGroup;
  /** Shows a spinner on the button while the login request is in flight */
  loading = false;

  constructor(
    private fb: FormBuilder,          // to build the form
    private authService: AuthService, // handles API calls & auth state
    private router: Router,           // for programmatic navigation
    private toastr: ToastrService     // for success / error toast messages
  ) {
    // Build the form with validators
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      remember: [false]   // default unchecked
    });
  }

  // -----------------------------------------------------------------
  // Convenience getters – used in the template to avoid long expressions
  // -----------------------------------------------------------------
  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }

  /** Called when the user clicks the Login button */
  onSubmit(): void {
    // If any field is invalid, mark all as touched to show errors
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;                 // show spinner, disable button
    const { email, password, remember } = this.loginForm.value;

    // Call AuthService login method (returns an Observable)
    this.authService.login(email, password).subscribe({
      next: (response) => {
        // If "remember me" is checked, store the email for next visit
        if (remember) {
          localStorage.setItem('rememberEmail', email);
        }
        this.toastr.success('Login successful');
        // Navigation is handled inside AuthService based on user status
        // (commented line kept for reference)
        // this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;
        // Show backend message if available, otherwise generic error
        this.toastr.error(error.error?.message || 'Login failed');
      },
      complete: () => {
        this.loading = false;   // always hide spinner when request finishes
      }
    });
  }
}