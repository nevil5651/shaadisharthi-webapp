import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, finalize } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RouterModule } from '@angular/router';

// Handles "Forgot Password" — sends reset link to user's email
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.html',
  styleUrls: ['./forgot-password.scss']
})
export class ForgotPasswordComponent {
  forgotPasswordForm: FormGroup;
  loading = false;           // For button spinner
  message: string | null = null;
  error: string | null = null;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    // Simple form with email validation
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  // Getter for template
  get email() {
    return this.forgotPasswordForm.get('email');
  }

  // Submit: send email to backend
  onSubmit() {
    if (this.forgotPasswordForm.invalid) return;

    this.loading = true;
    this.message = null;
    this.error = null;

    const email = this.forgotPasswordForm.value.email;
    const body = `email=${encodeURIComponent(email)}`;
    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    this.http.post(`${environment.apiUrl}/ServiceProvider/forgot-password`, body, { headers })
      .pipe(
        catchError(error => {
          this.error = error.error?.error || 'Failed to send reset email. Please try again.';
          return throwError(() => error);
        }),
        finalize(() => this.loading = false)
      )
      .subscribe(() => {
        this.message = 'If your email is registered, a reset link has been sent.';
        this.forgotPasswordForm.reset();  // Clear form
      });
  }
}