package shaadisharthi.dto;

/**
 * BookingNotification - Data Transfer Object for booking notification events
 * 
 * Used for real-time notification system to communicate booking events:
 * - WebSocket notifications
 * - Event-driven architecture
 * - Real-time client updates
 * 
 * Contains minimal payload for efficient network transmission
 * 
 * @category Data Transfer Objects
 * @see BookingStatusUpdate
 */
public class BookingNotification {
    private String bookingId;
    private String type;

    /**
     * Default constructor for JSON deserialization
     */
    public BookingNotification() {}

    /**
     * Parameterized constructor for easy object creation
     * 
     * @param bookingId Unique identifier for the booking
     * @param type Type of notification (e.g., "CREATED", "UPDATED", "CANCELLED")
     */
    public BookingNotification(String bookingId, String type) {
        this.bookingId = bookingId;
        this.type = type;
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
     * @return Notification type indicating the nature of the event
     */
    public String getType() {
        return type;
    }

    /**
     * @param type Notification type (e.g., "CREATED", "UPDATED", "CANCELLED")
     */
    public void setType(String type) {
        this.type = type;
    }
}