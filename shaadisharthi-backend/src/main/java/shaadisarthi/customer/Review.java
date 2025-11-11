package shaadisarthi.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.Timestamp;

import org.json.JSONObject;

/**
 * Data model representing customer reviews for services
 * Used for storing and retrieving review data with JSON serialization
 * 
 * @JsonInclude Excludes null fields from JSON serialization
 * @version 1.0
 * @description POJO for service reviews with timestamp and customer information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Review {
    private int reviewId;
    private int serviceId;
    private int customerId;
    private String customerName;
    private String reviewText;
    private int rating;
    private Timestamp createdAt;

    // Getters and Setters
    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Converts Review object to JSON string representation
     * 
     * @return JSON string of the review data
     */
    @Override
    public String toString() {
        return new JSONObject(this).toString();
    }
}