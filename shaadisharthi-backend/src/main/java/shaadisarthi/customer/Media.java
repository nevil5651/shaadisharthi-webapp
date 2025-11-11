package shaadisarthi.customer;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data model representing media associated with services
 * Used for storing and retrieving service images and other media files
 * 
 * @JsonInclude Excludes null fields from JSON serialization
 * @version 1.0
 * @description POJO for service media with Jackson annotations for JSON handling
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Media {
    private int mediaId;
    private int serviceId;
    private String mediaType;
    private String mediaUrl;

    // Getters and Setters
    public int getMediaId() { return mediaId; }
    public void setMediaId(int mediaId) { this.mediaId = mediaId; }
    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}