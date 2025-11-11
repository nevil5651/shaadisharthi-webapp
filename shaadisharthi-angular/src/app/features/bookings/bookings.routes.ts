// Import Angular's routing module to define routes
import { Routes } from '@angular/router';
// Import the two main booking components
import { BookingsComponent } from './bookings';
import { ConfirmedBookingsComponent } from '../confirmed-bookings/confirmed-bookings';

// Define routes for the bookings module
export const BOOKINGS_ROUTES: Routes = [
  {
    path: 'pending',
    component: BookingsComponent,
    title: 'Pending Bookings'  // Sets browser tab title
  },
  {
    path: 'confirmed',
    component: ConfirmedBookingsComponent,
    title: 'Confirmed Bookings'  // Sets browser tab title
  },
  {
    path: '',
    redirectTo: 'pending',  // Default route: go to pending if no path specified
    pathMatch: 'full'       // Ensures full match for empty path
  }
];