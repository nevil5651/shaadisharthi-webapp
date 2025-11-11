package shaadisharthi.dto;

/**
 * BookingStatusUpdate - Data Transfer Object for booking status changes
 * 
 * Used to communicate booking status transitions throughout the system:
 * - Status workflow updates (Pending → Accepted → Completed)
 * - Real-time status synchronization
 * - Client-side status display updates
 * 
 * @category Data Transfer Objects
 * @see BookingNotification
 */
public class BookingStatusUpdate {
    private String bookingId;
    private String status;

    /**
     * Default constructor for JSON deserialization
     */
    public BookingStatusUpdate() {}

    /**
     * Parameterized constructor for easy object creation
     * 
     * @param bookingId Unique identifier for the booking
     * @param status Current booking status (e.g., "Pending", "Accepted", "Completed", "Cancelled")
     */
    public BookingStatusUpdate(String bookingId, String status) {
        this.bookingId = bookingId;
        this.status = status;
    }

    // Getters and setters
    
    /**
     * @return Unique booking identifier
     */
    public String getBookingId() {
        return bookingId;
    }

    /**
     * @param bookingId Unique booking identifier
     */
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    /**
     * @return Current booking status in the workflow
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status New booking status (e.g., "Pending", "Accepted", "Completed")
     */
    public void setStatus(String status) {
        this.status = status;
    }
}