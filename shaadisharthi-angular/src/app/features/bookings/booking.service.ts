// booking.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BookingsResponse } from './models/booking.model';
 
// Use environment file for API base URL (dev/prod switching)
import { environment } from '../../../environments/environment';

// Service is provided at root level — available app-wide
@Injectable({
  providedIn: 'root'
})
export class BookingService {

  // Base API URL from environment (fallback to localhost)
  private apiUrl = environment.apiUrl || 'http://localhost:3000/api';

  constructor(private http: HttpClient) {}

  /**
   * Fetches a paginated list of pending bookings.
   * @param page The page number to fetch.
   * @param limit The number of items per page.
   * @returns An Observable of the bookings response.
   */
  getPendingBookings(page: number, limit: number): Observable<BookingsResponse> {
    const params = new HttpParams()
      .set('status', 'Pending')
      .set('page', page.toString())
      .set('limit', limit.toString());

    return this.http.get<BookingsResponse>(`${this.apiUrl}/ServiceProvider/booking-list-servlet`, { params })
      .pipe(
        catchError(this.handleError)  // Handle any HTTP errors
      );
  }

  /**
   * Fetches a paginated list of confirmed bookings.
   * @param page The page number to fetch.
   * @param limit The number of items per page.
   * @returns An Observable of the bookings response.
   */
  getConfirmedBookings(page: number, limit: number): Observable<BookingsResponse> {
    const params = new HttpParams()
      .set('status', 'Confirmed')
      .set('page', page.toString())
      .set('limit', limit.toString());

    return this.http.get<BookingsResponse>(`${this.apiUrl}/ServiceProvider/booking-list-servlet`, { params })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Sends a request to accept a booking.
   * @param bookingId The ID of the booking to accept.
   * @returns An Observable of the API response.
   */
  acceptBooking(bookingId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/ServiceProvider/booking-action/${bookingId}`, { action: 'accept' })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Sends a request to reject a booking.
   * @param bookingId The ID of the booking to reject.
   * @param reason An optional reason for rejection.
   * @returns An Observable of the API response.
   */
  rejectBooking(bookingId: number, reason?: string): Observable<any> {
    const payload: { action: string; reason?: string } = { action: 'reject' };
    if (reason) { payload.reason = reason; }
    return this.http.post<any>(`${this.apiUrl}/ServiceProvider/booking-action/${bookingId}`, payload)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Cancels a confirmed booking.
   * @param bookingId The ID of the booking to cancel.
   * @param reason An optional reason for cancellation.
   * @returns An Observable of the API response.
   */
  cancelBooking(bookingId: number, reason?: string): Observable<any> {
    const payload: { action: string; reason?: string } = { action: 'cancel' };
    if (reason) { payload.reason = reason; }
    return this.http.post<any>(`${this.apiUrl}/ServiceProvider/booking-action/${bookingId}`, payload)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Marks a booking as completed.
   * @param bookingId The ID of the booking to mark as completed.
   * @returns An Observable of the API response.
   */
  completeBooking(bookingId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/ServiceProvider/booking-action/${bookingId}`, { action: 'complete' })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * A centralized error handler for HTTP requests.
   * @param error The HttpErrorResponse.
   * @returns An Observable that throws an error.
   */
  private handleError(error: HttpErrorResponse) {
    if (error.status === 0) {
      console.error('An error occurred:', error.error);
    } else {
      console.error(`Backend returned code ${error.status}, body was: `, error.error);
    }
    return throwError(() => new Error('Something bad happened; please try again later.'));
  }
}