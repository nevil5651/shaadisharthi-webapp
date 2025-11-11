// Interface for a single booking item
export interface Booking {
  bookingId: number;           // Unique ID of the booking
  customerName: string;        // Name of the customer
  serviceName: string;         // Name of the booked service
  eventStartDate: string;      // ISO date string: "2026-11-11 11:01:00.0"
  totalAmount: number;         // Total price
  phone: string;               // Customer phone (not used in UI yet)
}

// Response structure from the backend API
export interface BookingsResponse {
  bookings: Booking[];         // Array of booking objects
  total: number;               // Total count (for pagination)
  page: number;                // Current page
  limit: number;               // Items per page
}