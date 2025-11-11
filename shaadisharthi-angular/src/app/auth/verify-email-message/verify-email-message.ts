import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth';
import { ToastrService } from 'ngx-toastr';

// Shown after verification email is sent
@Component({
  selector: 'app-verify-email-message',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify-email-message.html',
})
export class VerifyEmailMessageComponent implements OnInit {
  email: string = '';         // Email to display
  isResending = false;        // Resend loading state

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    // Get email from URL query param, fallback to localStorage
    this.email = this.route.snapshot.queryParamMap.get('email') 
                 || localStorage.getItem('pendingEmail') 
                 || 'your email';

    // If no email found, send back to start
    if (!this.email || this.email === 'your email') {
      this.router.navigate(['/email-verification']);
    }
  }

  // Resend verification email
  resendEmail() {
    if (this.email) {
      this.isResending = true;
      this.authService.requestVerificationEmail(this.email).subscribe({
        next: () => {
          this.isResending = false;
          this.toastr.success('Verification email sent successfully!', 'Success');
        },
        error: () => {
          this.isResending = false;
          this.toastr.error('Failed to send verification email. Please try again later.', 'Error');
        },
      });
    }
  }
}