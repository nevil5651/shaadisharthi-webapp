package shaadisarthi.customer;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data model representing service offerings in the platform
 * Contains comprehensive service details including pricing, ratings, and provider information
 * 
 * @JsonInclude Excludes null fields from JSON serialization
 * @version 1.0
 * @description POJO for service data with business information and metrics
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Service {
    private int serviceId;
    private String serviceName;
    private String category;
    private String location;
    private Double rating;
    private int reviewCount;
    private double price;
    private String imageUrl;
    private String businessName;
    private String description;
    private String email;
    private String phone;

    // Getters and Setters
    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Converts Service object to JSON string representation
     * 
     * @return JSON string of the service data
     */
    @Override
    public String toString() {
        return new JSONObject(this).toString();
    }
}