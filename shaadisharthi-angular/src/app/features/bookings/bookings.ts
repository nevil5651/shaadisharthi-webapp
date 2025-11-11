// Core Angular lifecycle and change detection imports
import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
// Material Dialog for confirmation popups
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
// Virtual scrolling for performance with large lists
import { CdkVirtualScrollViewport, ScrollingModule } from '@angular/cdk/scrolling';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
// RxJS for handling async operations and cleanup
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
// Toastr for success/error notifications
import { ToastrService } from 'ngx-toastr';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Service to handle API calls
import { BookingService } from './booking.service';
import { Booking } from './models/booking.model';
// Shared confirmation dialog
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog';

// Standalone component for pending bookings
@Component({
  selector: 'app-bookings',
  standalone: true,
  imports: [
    CommonModule,
    ScrollingModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './bookings.html',
  styleUrl: './bookings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush  // Optimize performance: only check when input changes
})
export class BookingsComponent implements OnInit, OnDestroy {
  bookings: Booking[] = [];
  page = 1;                  // Current page for pagination
  limit = 20;                // Items per page
  total = 0;                 // Total number of bookings from server
  loading = false;           // Tracks if data is being loaded
  displayedColumns: string[] = ['bookingId', 'customerName', 'serviceName', 'eventStartDate', 'totalAmount', 'actions'];

  // Subject to notify when component is destroyed (cleanup)
  private readonly destroy$ = new Subject<void>();
  // Reference to the virtual scroll viewport
  @ViewChild(CdkVirtualScrollViewport) viewport!: CdkVirtualScrollViewport;

  // Dependency injection: services used in this component
  constructor(
    private bookingService: BookingService,
    // private authService: AuthService, // Uncomment when AuthService is ready
    public dialog: MatDialog,
    private cdr: ChangeDetectorRef,
    private toastr: ToastrService
  ) {}

  // Load bookings when component initializes
  ngOnInit(): void {
    this.loadBookings(false); // false = replace data, not append
  }

  // Cleanup subscriptions when component is destroyed
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Fetch bookings from server
  loadBookings(append = true): void {
    if (this.loading) return;  // Prevent multiple simultaneous requests
    this.loading = true;

    this.bookingService.getPendingBookings(this.page, this.limit)
      .pipe(takeUntil(this.destroy$))  // Auto-unsubscribe on destroy
      .subscribe({
        next: (data) => {
          // Replace or append new bookings
          this.bookings = append ? [...this.bookings, ...data.bookings] : data.bookings;
          this.total = data.total;
          this.loading = false;
          this.cdr.markForCheck(); // Manually trigger change detection (OnPush)
        },
        error: (err) => {
          this.loading = false;
          this.toastr.error('Error loading bookings', 'Error');
          console.error('API error: ', err);
          this.cdr.markForCheck(); // Update UI even on error
        }
      });
  }

  // Triggered when user scrolls — load more data if at bottom
  onScroll(): void {
    if (!this.viewport || this.loading) return; 
    const end = this.viewport.getRenderedRange().end;
    const totalDataLength = this.viewport.getDataLength();

    // If scrolled to the end and more data exists
    if (end === totalDataLength && this.bookings.length < this.total) {
      this.page++;
      this.loadBookings();  // append = true by default
    }
  }

  // Open confirmation dialog before accepting
  acceptBooking(bookingId: number): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: { title: 'Confirm Acceptance', message: 'Are you sure you want to accept this booking?' }
    });

    dialogRef.afterClosed()
    .pipe(takeUntil(this.destroy$))
    .subscribe(result => {
      if (result?.confirm) {
        this.bookingService.acceptBooking(bookingId)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              // Remove accepted booking from list
              this.bookings = this.bookings.filter(b => b.bookingId !== bookingId);
              this.toastr.success('Booking accepted successfully', 'Success');
              this.cdr.markForCheck();
            },
            error: () => this.toastr.error('Error accepting booking', 'Error')
          });
      }
    });
  }

  // Open dialog with reason input for rejection
  rejectBooking(bookingId: number): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: { title: 'Confirm Rejection', message: 'Are you sure you want to reject this booking?', showInput: true }
    });

    dialogRef.afterClosed()
    .pipe(takeUntil(this.destroy$))
    .subscribe(result => {
      if (result?.confirm) {
        this.bookingService.rejectBooking(bookingId, result.reason)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.bookings = this.bookings.filter(b => b.bookingId !== bookingId);
              this.toastr.success('Booking rejected successfully', 'Success');
              this.cdr.markForCheck();
            },
            error: () => this.toastr.error('Error rejecting booking', 'Error')
          });
      }
    });
  }
}