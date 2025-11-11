package shaadisharthi.admin;

/**
 * Customer Data Model Class
 * 
 * Represents a customer entity in the ShaadiSharthi platform with comprehensive profile information.
 * Used for transferring customer data between database, services, and presentation layers.
 * 
 * Fields include:
 * - Basic identification: customerId, name, email
 * - Contact information: phone numbers and address
 * - Timestamp: account creation date
 * 
 * This is a POJO (Plain Old Java Object) with standard getters and setters
 * for data encapsulation and easy JSON serialization.
 */
public class Customer {
    private int customerId;
    private String name;
    private String email;
    private String createdAt;
    private String phoneNumber;
    private String alternatePhoneNumber;
    private String address;

    // Standard getters and setters for all fields
    // These provide controlled access to the private fields and enable
    // frameworks to serialize/deserialize the object
    
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAlternatePhoneNumber() { return alternatePhoneNumber; }
    public void setAlternatePhoneNumber(String alternatePhoneNumber) { this.alternatePhoneNumber = alternatePhoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}