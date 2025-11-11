package shaadisharthi.admin;

/**
 * ServiceProvider - Data model representing service provider entity
 * 
 * Contains comprehensive profile information for wedding service providers:
 * - Personal details (name, email, contact information)
 * - Business details (business name, GST, Aadhar, PAN)
 * - Location information (address, city, state)
 * - Registration metadata (creation date, approval status)
 * 
 * Used throughout the service provider management system for data transfer
 * and JSON serialization.
 * 
 * @category Data Models
 */
public class ServiceProvider {
    private int providerId;
    private String name;
    private String email;
    private String createdAt;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String alternatePhoneNumber;
    private String businessName;
    private String gstNumber;
    private String aadharNumber;
    private String panNumber;
    private String status; // Added status field

    // Getters and Setters
    
    /**
     * @return Unique provider identifier
     */
    public int getProviderId() { return providerId; }
    public void setProviderId(int providerId) { this.providerId = providerId; }

    /**
     * @return Provider's full name
     */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /**
     * @return Provider's email address (used for communication)
     */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /**
     * @return Registration timestamp in string format
     */
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * @return Primary 10-digit phone number
     */
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /**
     * @return Physical address of provider/business
     */
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    /**
     * @return City where provider operates
     */
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    /**
     * @return State where provider operates
     */
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    /**
     * @return Alternate contact number (optional)
     */
    public String getAlternatePhoneNumber() { return alternatePhoneNumber; }
    public void setAlternatePhoneNumber(String alternatePhoneNumber) { this.alternatePhoneNumber = alternatePhoneNumber; }

    /**
     * @return Registered business name
     */
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    /**
     * @return GST identification number
     */
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    /**
     * @return Aadhar card number (12 digits)
     */
    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    /**
     * @return PAN card number
     */
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    /**
     * @return Current approval status (basic_registered, pending_approval, approved, rejected)
     */
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}