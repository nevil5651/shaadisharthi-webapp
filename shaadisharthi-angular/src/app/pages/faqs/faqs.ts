import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastrService } from 'ngx-toastr';
import { Faq, FaqQuery, FaqService } from '../../core/services/faq';
import { Observable, EMPTY } from 'rxjs';
import { finalize, take, tap } from 'rxjs/operators';

// This is the main component for the FAQs page
@Component({
  selector: 'app-faqs',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './faqs.html',
  styleUrls: ['./faqs.scss'],
})
export class FaqsComponent implements OnInit {
  // Injecting dependencies using Angular's inject function
  private fb = inject(FormBuilder);
  private faqService = inject(FaqService);
  private toastr = inject(ToastrService);

  // Observable to hold the list of FAQs, starts empty
  faqs$: Observable<Faq[]> = EMPTY;
  // Flag to show if the form is being submitted
  isSubmitting = false;
  // Flag to track if the modal is open
  isModalOpen = false;

  // Setting up the reactive form for the query submission
  // Initialize the form directly for better type inference and to avoid definite assignment assertions.
  queryForm: FormGroup = this.fb.group({
    subject: ['', [Validators.required, Validators.minLength(5)]],
    message: ['', [Validators.required, Validators.minLength(20)]],
  });

  // Add getters for easy access to form controls in the template.
  // This avoids using `queryForm.get('subject')` in the HTML.
  get subject(): AbstractControl | null {
    return this.queryForm.get('subject');
  }

  get message(): AbstractControl | null {
    return this.queryForm.get('message');
  }

  // Lifecycle hook that runs when the component initializes
  ngOnInit(): void {
    // This line is required for the FAQ list in your HTML to display questions.
    
  }

  /**
   * This function is called when the form is submitted.
   * It handles validation and calls the service to send the data to your API.
   */
  submitQuery(): void {
    // Mark all fields as touched to trigger validation messages in the template.
    this.queryForm.markAllAsTouched();

    // If the form is invalid, stop here
    if (this.queryForm.invalid) {
      return;
    }

    // Set submitting flag to true
    this.isSubmitting = true;

    // Trim values before sending to the API for better data integrity.
    const formValue: FaqQuery = {
      subject: this.queryForm.value.subject.trim(),
      message: this.queryForm.value.message.trim(),
    };

    // Call the service to add the query
    this.faqService.addQuery(formValue)
      .pipe(
        take(1), // Ensure the subscription automatically completes after one emission.
        tap(() => {
          // Show success message and close modal on success
          this.toastr.success('Your query has been submitted successfully!');
          this.closeModal();
        }),
        finalize(() => this.isSubmitting = false) // Guarantees this runs on success, error, or completion.
      )
      .subscribe({
        error: (err) => {
          // Handle error by showing toast and logging
          this.toastr.error('Failed to submit your query. Please try again.', 'API Error');
          console.error('Error submitting query:', err);
        },
      });
  }

  // Function to close the modal and reset the form
  closeModal(): void {
    this.isModalOpen = false;
    this.queryForm.reset(); // Reset form state when closing the modal.
  }
}