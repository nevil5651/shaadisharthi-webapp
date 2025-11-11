import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';                              // For Angular directives
import { AuthService } from '../../core/services/auth';                       // Service for auth/profile
import { ChangePasswordPayload, User, UserProfile } from '../../core/models/auth.model';  // Data models
import { ReactiveFormsModule } from '@angular/forms';                         // For reactive forms
import { ToastrService } from 'ngx-toastr';                                   // For notifications
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';  // Form tools
import { Subscription } from 'rxjs';                                           // For subscriptions

// Account/Profile component — handles viewing and editing user info
@Component({
  selector: 'app-account',                     // Use <app-account></app-account>
  imports: [CommonModule, ReactiveFormsModule],  // Required for forms and directives
  templateUrl: './account.html',               // HTML template
  styleUrl: './account.scss'                   // SCSS styles
})
export class Account implements OnInit, OnDestroy {
  accountData: User | null = null;             // Current user data
  editForm: FormGroup;                         // Form for editing profile
  passwordForm: FormGroup;                     // Form for changing password
  loading: boolean;                            // Loading state for buttons
  private userSubscription: Subscription | undefined;  // Subscription to user data

  // Constructor: Setup forms and inject services
  constructor(
    private authService: AuthService,          // For profile updates and user stream
    private fb: FormBuilder,                   // For building forms
    private toastr: ToastrService              // For success/error toasts
  ) {
    // Profile edit form with validators
    this.editForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],         // Required, max 100 chars
      state: ['', [Validators.required, Validators.maxLength(100)]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],  // 10-digit phone
      alternate_phone: ['', Validators.pattern('^[0-9]{10}$')],             // Optional 10-digit
      business_name: ['', [Validators.required, Validators.maxLength(100)]],
      address: ['', [Validators.required, Validators.maxLength(255)]],
    });
    // Password change form with validators
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],                           // Required
      newPassword: ['', [                                                   // Complex password rules
        Validators.required, Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/)  // Upper, lower, digit, special
      ]],
      renewPassword: ['', Validators.required]                              // Required
    }, { validators: this.passwordMatchValidator });                        // Custom validator for match
    this.loading = false;                                                   // Initial loading false
  }

  // Lifecycle: Subscribe to user data on init
  ngOnInit(): void {
    this.userSubscription = this.authService.currentUser$.subscribe((user: User | null) => {
      if (user) {
        this.accountData = user;              // Update local data
        this.populateEditForm();              // Fill form with current values
      }
    });
  }

  // Custom validator: Check if new passwords match
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const renewPassword = control.get('renewPassword')?.value;
    return newPassword === renewPassword ? null : { mismatch: true };  // Return error if mismatch
  }

  // Fill edit form with current user data
  populateEditForm(): void {
    if (this.accountData) {
      this.editForm.patchValue(this.accountData);  // Set form values
    }
  }

  // Submit password change form
  onChangePasswordSubmit(): void {
    if (this.passwordForm.invalid) {               // If invalid, show errors
      this.displayPasswordFormErrors();
      return;
    }

    this.loading = true;                           // Start loading
    const passwordData: ChangePasswordPayload = this.passwordForm.value;  // Get form data
    this.authService.changePassword(passwordData).subscribe({
      next: (response) => {                        // Success
        console.log('Password changed successfully', response);
        this.toastr.success('Password changed successfully, logging you out!');
        this.authService.logout();                 // Logout after change
      },
      error: (error) => {                          // Error
        console.error('Password change failed', error);
        const errorMessage = error?.error?.message || 'Password change failed. Please check your current password.';
        this.toastr.error(errorMessage);
      },
      complete: () => {                            // Always
        this.loading = false;                      // Stop loading
      }
    });
  }

  // Show toast errors for password form
  private displayPasswordFormErrors(): void {
    const newPasswordControl = this.passwordForm.get('newPassword');

    // Check form-level errors first
    if (this.passwordForm.hasError('mismatch')) {
      this.toastr.error('New passwords do not match.');
    } else if (this.passwordForm.get('currentPassword')?.hasError('required')) {
      this.toastr.error('Current password is required.');
    } else if (newPasswordControl?.hasError('required')) {
      this.toastr.error('New password is required.');
    } else if (newPasswordControl?.hasError('minlength')) {
      this.toastr.error('Password must be at least 8 characters long.');
    } else if (newPasswordControl?.hasError('pattern')) {
      this.toastr.error('Password must contain an uppercase letter, a lowercase letter, a number, and a special character.');
    } else if (this.passwordForm.get('renewPassword')?.hasError('required')) {
      this.toastr.error('Please re-enter the new password.');
    }
  }

  // Submit profile edit form
  onEditSubmit(): void {
    if (this.editForm.valid) {                     // If valid
      this.loading = true;                         // Start loading
      const updatedData: Partial<UserProfile> = this.editForm.value;  // Get changes
      this.authService.updateProfile(updatedData).subscribe({
        next: (response) => {                      // Success
          console.log('Profile updated successfully', response);
          this.toastr.success('Profile updated successfully!');
          this.loading = false;
        },
        error: (error) => {                        // Error
          console.error('Profile update failed', error);
          this.toastr.error('Profile update failed. Please try again.');
          this.loading = false;
        }
      });
    } else {
      console.error('Form is invalid');            // Log if invalid
    }
  }

  // Cleanup: Unsubscribe on destroy
  ngOnDestroy(): void {
    this.userSubscription?.unsubscribe();          // Prevent memory leaks
  }
}