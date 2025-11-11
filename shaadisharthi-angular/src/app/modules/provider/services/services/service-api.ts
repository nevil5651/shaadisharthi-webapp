// This service provides API methods for interacting with the backend regarding services.
// It handles CRUD operations for services and media, plus signature requests for uploads.

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Service, Media } from '../models/service.model';
import { environment } from '../../../../../environments/environment';

// Making this service injectable at the root level.
@Injectable({
  providedIn: 'root'
})

export class ServiceApiService {

  // Base URLs for different API endpoints, constructed from environment config.
  private apiUrl = `${environment.apiUrl}/ServiceProvider/providerservices`;
  private crturl = `${environment.apiUrl}/ServiceProvider/createservice` ;
  private edturl = `${environment.apiUrl}/ServiceProvider/editservice` ;
  private dlturl = `${environment.apiUrl}/ServiceProvider/deleteservice` ;

  // Injecting HttpClient for making HTTP requests.
  constructor(private http: HttpClient) {}

  // Fetches all services for the current provider.
  getServices(): Observable<Service[]> {
    return this.http.get<Service[]>(this.apiUrl);
  }

  // Fetches a single service by its ID.
  getService(serviceId: number): Observable<Service> {
    return this.http.get<Service>(`${this.apiUrl}/${serviceId}`);
  }

  // Creates a new service with the provided data.
  createService(serviceData: Partial<Service>): Observable<Service> {
    return this.http.post<Service>(`${this.crturl}`, serviceData);
  }

  // Updates an existing service by ID with partial data.
  updateService(serviceId: number, serviceData: Partial<Service>): Observable<Service> {
    return this.http.put<Service>(`${this.edturl}/${serviceId}`, serviceData);
  }

  // Adds new media to a service.
  addMedia(serviceId: number, mediaData: { url: string; type: 'image' | 'video'; fileSize: number; fileExtension: string; }): Observable<Media> {
    return this.http.post<Media>(`${this.edturl}/${serviceId}/media`, mediaData);
  }

  // Deletes a specific media item from a service.
  deleteMedia(serviceId: number,mediaId: number): Observable<void> {
    // This URL should be relative to your main API endpoint for consistency.
    // Adjust if your backend has a different structure.
    return this.http.delete<void>(`${this.edturl}/${serviceId}/media/${mediaId}`);
  }

  // Deletes an entire service by ID.
  deleteService(serviceId: number): Observable<void> {
    return this.http.delete<void>(`${this.dlturl}/${serviceId}`);
  }

  /**
   * Requests a signature from the backend for a secure Cloudinary upload.
   * The backend signs the parameters passed in the request body.
   * The backend must have an endpoint that generates this signature.
   * @param paramsToSign An object of parameters to be included in the signature.
   */

  getCloudinarySignature(paramsToSign: object): Observable<{ signature: string; timestamp: number; apiKey: string }> {
    return this.http.post<{ signature: string; timestamp: number; apiKey: string }>(`${environment.apiUrl}/ServiceProvider/cloudinarysignature`, paramsToSign);
  }

}