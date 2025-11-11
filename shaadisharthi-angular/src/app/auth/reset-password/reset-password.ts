import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { throwError } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// Custom validator: ensures password and confirmPassword match
export function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { mismatch: true };
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.scss']
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm: FormGroup;
  loading = false;
  message: string | null = null;
  error: string | null = null;
  token: string | null = null;  // From URL ?token=...

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.resetPasswordForm = this.fb.group({
      password: ['', [
        Validators.required,
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  // Get token from URL on init
  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.token = params.get('token');
      if (!this.token) {
        this.error = 'Invalid or missing reset token.';
        this.resetPasswordForm.disable();
      }
    });
  }

  get password() { return this.resetPasswordForm.get('password'); }
  get confirmPassword() { return this.resetPasswordForm.get('confirmPassword'); }

  // Submit new password
  onSubmit(): void {
    if (this.resetPasswordForm.invalid || !this.token) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.message = null;
    this.error = null;

    const newPassword = this.password?.value;
    const body = { token: this.token, password: newPassword };

    this.http.post(`${environment.apiUrl}/ServiceProvider/reset-password`, body)
      .pipe(
        catchError(err => {
          this.error = err.error?.error || 'Failed to reset password. The link may have expired or is invalid.';
          return throwError(() => err);
        }),
        finalize(() => this.loading = false)
      )
      .subscribe(() => {
        this.message = 'Password has been reset successfully. Redirecting to login...';
        this.resetPasswordForm.disable();
        setTimeout(() => this.router.navigate(['/login']), 3000);  // Auto redirect
      });
  }
}