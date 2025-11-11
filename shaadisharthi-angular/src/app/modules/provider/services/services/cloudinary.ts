// This service handles secure file uploads to Cloudinary.
// It uses a backend-generated signature to authorize uploads directly from the browser,
// keeping sensitive keys secure.

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ServiceApiService } from './service-api';
import { environment } from '../../../../../environments/environment';

// Interface for the response from Cloudinary after a successful upload.
export interface CloudinaryUploadResponse {
  public_id: string; // Public ID of the uploaded asset.
  secure_url: string; // Secure URL to access the asset.
  // Add other response properties you need
}

// Making this service injectable and available at the root level.
@Injectable({
  providedIn: 'root'
})

export class CloudinaryUploadService {
  // Injecting dependencies: HttpClient for API calls and ServiceApiService for signatures.
  private readonly http = inject(HttpClient);
  private readonly apiService = inject(ServiceApiService);

  /**
   * Orchestrates a secure, signed upload directly to Cloudinary.
   * This method first obtains a temporary, secure signature from the backend,
   * then uses that signature to authorize a direct file upload from the
   * browser to Cloudinary, ensuring the API secret never leaves the server.
   *
   * @param file The file to upload.
   * @returns An observable of the Cloudinary upload response.
   */

  upload(file: File): Observable<CloudinaryUploadResponse> {
    // Determine the resource type and destination folder based on the file's MIME type.
    const isVideo = file.type.startsWith('video/');
    const resourceType = isVideo ? 'video' : 'image';
    const folder = isVideo ? 'shaadisharthi/videos' : 'shaadisharthi/images';

    // Constructing the Cloudinary upload URL using environment config.
    const uploadUrl = `https://api.cloudinary.com/v1_1/${environment.cloudinary.cloudName}/${resourceType}/upload`;
    
    // These are the parameters we want the backend to include in the signature.
    // Any parameter signed on the backend MUST be sent in the FormData here.
    const paramsToSign = {
      folder: folder
      // You could add other parameters here, like a public_id, if you wanted
      // the backend to sign them too.
    };

    // Step 1: Get the signature from our secure backend for these specific params.
    return this.apiService.getCloudinarySignature(paramsToSign).pipe(
      switchMap((sigResponse) => {
        // Preparing FormData for the upload request.
        const formData = new FormData();

        // The file itself.
        formData.append('file', file);

        // These parameters prove to Cloudinary that the request is authentic and recent.
        formData.append('api_key', sigResponse.apiKey);
        formData.append('timestamp', sigResponse.timestamp.toString());
        formData.append('signature', sigResponse.signature);

        // IMPORTANT: Append all parameters that were part of the signature generation.
        formData.append('folder', paramsToSign.folder);

        // Step 2: Post the file and signature data directly to Cloudinary's API endpoint.
        return this.http.post<CloudinaryUploadResponse>(uploadUrl, formData);
      })
    );
  }
}