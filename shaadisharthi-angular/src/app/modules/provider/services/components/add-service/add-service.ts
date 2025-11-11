// add-service.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { finalize } from 'rxjs';
import { ServiceStateService } from '../../services/service-state.service';
import { CloudinaryUploadService, CloudinaryUploadResponse } from '../../services/cloudinary';

// -----------------------------------------------------------------------------
// COMPONENT: AddServiceComponent
// Handles creation of a new service + media upload (image/video) to Cloudinary.
// -----------------------------------------------------------------------------
@Component({
  selector: 'app-add-service',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './add-service.html',
  styleUrls: ['./add-service.scss']
})
export class AddServiceComponent implements OnInit {
  // -------------------------------------------------------------------------
  // Injected dependencies
  // -------------------------------------------------------------------------
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly serviceState = inject(ServiceStateService);
  private readonly toastr = inject(ToastrService);
  private readonly cloudinaryService = inject(CloudinaryUploadService);

  // -------------------------------------------------------------------------
  // Form & UI state
  // -------------------------------------------------------------------------
  public serviceForm!: FormGroup;
  public isSubmitting = false;
  public isUploading = false;

  // -------------------------------------------------------------------------
  // Size limits (customise here)
  // -------------------------------------------------------------------------
  private readonly MAX_IMAGE_SIZE_MB = 3;   // 5 MB max for images
  private readonly MAX_VIDEO_SIZE_MB = 19;  // 50 MB max for videos (adjust as needed)

  constructor() {}

  // -------------------------------------------------------------------------
  // Initialise the reactive form
  // -------------------------------------------------------------------------
  ngOnInit(): void {
    this.serviceForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      category: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      price: [null, [Validators.required, Validators.min(0)]],
      media: this.fb.array([]) // Holds uploaded media objects
    });
  }

  // -------------------------------------------------------------------------
  // Helper: convert bytes → MB (rounded to 2 decimals)
  // -------------------------------------------------------------------------
  private toMB(bytes: number): number {
    return Number((bytes / (1024 * 1024)).toFixed(2));
  }

  // -------------------------------------------------------------------------
  // Validate file type & size **before** uploading
  // Returns true if the file is acceptable, false otherwise.
  // -------------------------------------------------------------------------
  private validateFile(file: File): boolean {
    const type = file.type;

    // ---- TYPE CHECK -------------------------------------------------------
    const isImage = type.startsWith('image/');
    const isVideo = type.startsWith('video/');

    if (!isImage && !isVideo) {
      this.toastr.error('Only image and video files are allowed.');
      return false;
    }

    // ---- SIZE CHECK -------------------------------------------------------
    const sizeMB = this.toMB(file.size);
    if (isImage && sizeMB > this.MAX_IMAGE_SIZE_MB) {
      this.toastr.error(`Image size cannot exceed ${this.MAX_IMAGE_SIZE_MB} MB.`);
      return false;
    }
    if (isVideo && sizeMB > this.MAX_VIDEO_SIZE_MB) {
      this.toastr.error(`Video size cannot exceed ${this.MAX_VIDEO_SIZE_MB} MB.`);
      return false;
    }

    return true;
  }

  // -------------------------------------------------------------------------
  // Getter for the media FormArray
  // -------------------------------------------------------------------------
  get media(): FormArray {
    return this.serviceForm.get('media') as FormArray;
  }

  // -------------------------------------------------------------------------
  // File input change → validate → upload to Cloudinary
  // -------------------------------------------------------------------------
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    // ---- VALIDATE BEFORE ANY NETWORK CALL -------------------------------
    if (!this.validateFile(file)) {
      // Reset input so the same invalid file can be re-selected later
      input.value = '';
      return;
    }

    // ---- PROCEED WITH UPLOAD --------------------------------------------
    this.isUploading = true;
    this.cloudinaryService.upload(file)
      .pipe(finalize(() => {
        this.isUploading = false;
        input.value = ''; // allow same file again after upload
      }))
      .subscribe({
        next: (response: CloudinaryUploadResponse) => {
          this.addMediaControl(response, file);
          this.toastr.success('Media uploaded. Click "Save Service" to confirm.');
        },
        error: (err) => {
          this.toastr.error('Media upload failed. Please try again.', 'Upload Error');
          console.error('Cloudinary upload error:', err);
        }
      });
  }

  // -------------------------------------------------------------------------
  // Push a new media object into the FormArray after successful upload
  // -------------------------------------------------------------------------
  private addMediaControl(response: CloudinaryUploadResponse, file: File): void {
    const resourceType = file.type.startsWith('video') ? 'video' : 'image';
    const fileExtension = file.name.split('.').pop() || '';
    const fileSizeInMB = this.toMB(file.size); // consistent with previous Java impl

    const mediaGroup = this.fb.group({
      url: [response.secure_url, Validators.required],
      type: [resourceType, Validators.required],
      fileSize: [fileSizeInMB],
      fileExtension: [fileExtension]
    });
    this.media.push(mediaGroup);
  }

  // -------------------------------------------------------------------------
  // Remove media from the form (does NOT delete from Cloudinary)
  // -------------------------------------------------------------------------
  removeMedia(index: number): void {
    this.media.removeAt(index);
    this.toastr.info('Media removed from the form.');
  }

  // -------------------------------------------------------------------------
  // Submit the whole service (text fields + media URLs)
  // -------------------------------------------------------------------------
  onSubmit(): void {
    this.serviceForm.markAllAsTouched();
    if (this.serviceForm.invalid) {
      this.toastr.error('Please correct the errors in the form before submitting.');
      return;
    }

    this.isSubmitting = true;
    this.serviceState.addService(this.serviceForm.value).pipe(
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: () => {
        this.router.navigate(['/services']);
      },
      // Errors are already handled inside ServiceStateService
      error: (err) => console.error('Failed to create service', err)
    });
  }
}