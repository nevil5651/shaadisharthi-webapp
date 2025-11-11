package shaadisharthi;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model class representing a Service entity in the ShaadiSharthi system
 * Encapsulates service information including media assets for service providers
 * 
 * Features:
 * - Comprehensive service attributes
 * - Separate image and video path management
 * - Null-safe collection initialization
 * 
 * @author ShaadiSharthi Team
 * @version 1.0
 */
public class Service {
    private int id;
    private String name;
    private String category;
    private double price;
    private String description;
    private List<String> imagePaths; // List for image paths
    private List<String> videoPaths; // List for video paths

    /**
     * Default constructor initializes empty media collections
     * Ensures null-safe operations on media paths
     */
    public Service() {
        this.imagePaths = new ArrayList<>();
        this.videoPaths = new ArrayList<>();
    }

    // Getters and Setters

    /**
     * @return Service unique identifier
     */
    public int getId() {
        return id;
    }

    /**
     * @param id Service unique identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return Service name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Service name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Service category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category Service category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @return Service price
     */
    public double getPrice() {
        return price;
    }

    /**
     * @param price Service price
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * @return Service description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Service description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return List of image paths associated with the service
     */
    public List<String> getImagePaths() {
        return imagePaths;
    }

    /**
     * @param imagePaths List of image paths, initializes empty list if null
     */
    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = (imagePaths != null) ? imagePaths : new ArrayList<>();
    }

    /**
     * @return List of video paths associated with the service
     */
    public List<String> getVideoPaths() {
        return videoPaths;
    }

    /**
     * @param videoPaths List of video paths, initializes empty list if null
     */
    public void setVideoPaths(List<String> videoPaths) {
        this.videoPaths = (videoPaths != null) ? videoPaths : new ArrayList<>();
    }
}