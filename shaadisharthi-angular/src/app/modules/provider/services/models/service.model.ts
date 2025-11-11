// Defining interfaces for data models used in the services feature.
// Interface for Media items, which can be images or videos associated with a service.

export interface Media {
  id: number; // Unique identifier for the media.
  url: string; // URL where the media is hosted (e.g., on Cloudinary).
  type: 'image' | 'video'; // Type of media.
  fileSize: number;       // Add this // Size of the file in bytes.
  fileExtension: string; // File extension (e.g., 'jpg', 'mp4').
  // The backend will associate it with a service
}

// Interface for a Service, representing the main entity in this module.
export interface Service {
  id: number; // Unique identifier for the service.
  providerId: number; // ID of the provider who owns this service.
  name: string; // Name of the service.
  description: string; // Detailed description.
  category: string; // Category the service belongs to.
  price: number; // Price of the service.
  media: Media[]; // Array of media items associated with this service.
}