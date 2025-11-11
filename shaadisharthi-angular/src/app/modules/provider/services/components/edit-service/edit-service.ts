// Component for editing an existing service, handling form patching, media updates, and deletion.
import { Component, OnDestroy, OnInit, inject, DestroyRef } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute, } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ServiceStateService } from '../../services/service-state.service';
import { ToastrService } from 'ngx-toastr';
import { finalize, tap, filter, switchMap } from 'rxjs/operators';
import { Service, Media } from '../../models/service.model';
import { Observable, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// Defining the component as standalone with imports.
@Component({
  selector: 'app-edit-service',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './edit-service.html',
  styleUrls: ['./edit-service.scss']
})
export class EditService implements OnInit, OnDestroy {
  // Injecting dependencies.
  private readonly fb = inject(FormBuilder);
  private readonly serviceState = inject(ServiceStateService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toastr = inject(ToastrService);
  private readonly destroyRef = inject(DestroyRef);

  // Form group and service ID.
  serviceForm!: FormGroup;
  serviceId!: number;

  // Observables for service data and loading state.
  public readonly service$: Observable<Service | null>;
  public readonly isLoading$: Observable<boolean>;
  private serviceSubscription!: Subscription;

  // Flags for states.
  isSubmitting = false;
  isUploading = false;
  private isFormInitialized = false;

  // -------------------------------------------------------------------------
  // Size limits – customize as needed
  // -------------------------------------------------------------------------
  private readonly MAX_IMAGE_SIZE_MB = 3;   // 5 MB max for images
  private readonly MAX_VIDEO_SIZE_MB = 19;  // 50 MB max for videos

  constructor() {
    this.service$ = this.serviceState.selectedService$;
    this.isLoading$ = this.serviceState.isLoading$;
  }

  // -------------------------------------------------------------------------
  // Helper: convert bytes → MB (rounded to 2 decimals)
  // -------------------------------------------------------------------------
  private toMB(bytes: number): number {
    return Number((bytes / (1024 * 1024)).toFixed(2));
  }

  // -------------------------------------------------------------------------
  // Validate file type & size **before** upload
  // Returns true if valid, false otherwise.
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

  // Lifecycle hook to initialize form and subscribe to route/service changes.
  ngOnInit(): void {
    this.initializeForm();

    // Chain observables to get the ID, select the service, and then patch the form
    // once the service data is available.
    this.serviceSubscription = this.route.paramMap.pipe(
      tap(params => {
        this.serviceId = Number(params.get('id'));
        this.serviceState.selectService(this.serviceId);
      }),
      switchMap(() => this.serviceState.selectedService$),
      filter((service): service is Service => service !== null && service.id === this.serviceId)
    ).subscribe(service => {
      if (!this.isFormInitialized) {
        this.patchForm(service);
        this.isFormInitialized = true;
      } else {
        // On subsequent emissions (e.g., after media changes), only sync the media FormArray.
        this.syncMediaFormArray(service.media || []);
      }
    });
  }

  // Lifecycle hook to clean up subscription.
  ngOnDestroy(): void {
    // Clean up the subscription to prevent memory leaks.
    if (this.serviceSubscription) {
      this.serviceSubscription.unsubscribe();
    }
  }

  // Initializes the empty form.
  private initializeForm(): void {
    this.serviceForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      category: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      price: [null, [Validators.required, Validators.min(0)]],
      media: this.fb.array([]) // FormArray for media.
    });
  }

  // Patches the form with service data.
  private patchForm(service: Service): void {
    this.serviceForm.patchValue({
      name: service.name,
      category: service.category,
      description: service.description,
      price: service.price
    }, { emitEvent: false }); // Prevent infinite loops by not re-emitting valueChanges

    // Synchronize the media FormArray with the service's media
    this.media.clear();
    service.media?.forEach(m => this.addMediaControl(m));
  }

  // Getter for media FormArray.
  get media(): FormArray {
    return this.serviceForm.get('media') as FormArray;
  }

  // Creates a FormGroup for an existing media item.
  private addMediaControl(mediaItem: Media): void {
    const mediaGroup = this.fb.group({
      id: [mediaItem.id],
      url: [mediaItem.url, Validators.required],
      type: [mediaItem.type.toLowerCase(), Validators.required]
    });
    this.media.push(mediaGroup);
  }

  // Syncs only the media FormArray to prevent overwriting user input in other fields.
  private syncMediaFormArray(mediaItems: Media[]): void {
    // A simple check to prevent unnecessary processing if the media hasn't changed.
    if (this.media.length === mediaItems.length) {
      const formMediaIds = this.media.value.map((m: Media) => m.id).sort();
      const serviceMediaIds = mediaItems.map(m => m.id).sort();
      if (JSON.stringify(formMediaIds) === JSON.stringify(serviceMediaIds)) return;
    }
    this.media.clear({ emitEvent: false });
    mediaItems.forEach(m => this.addMediaControl(m));
  }

  // Handles file selection for adding new media.
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];

    // ---- VALIDATE BEFORE UPLOAD -----------------------------------------
    if (!this.validateFile(file)) {
      input.value = ''; // Reset to allow re-selection
      return;
    }

    this.isUploading = true;
    // The state service handles the entire flow: upload, API call, and state update.
    // The form will be updated automatically via the `selectedService$` subscription.
    this.serviceState.addMediaToService(this.serviceId, file).pipe(
      finalize(() => {
        this.isUploading = false;
        input.value = '';
      })
    ).subscribe({
      // Success/Error toasts are handled in the state service.
      error: (err) => console.error('Add media failed', err)
    });
  }

  // Handles media removal with confirmation.
  removeMedia(mediaId: number): void {
    if (!confirm('Are you sure you want to delete this media? This action is immediate and cannot be undone.')) {
      return;
    }
    // The state service handles the API call and state update.
    // The form will be updated automatically via the `selectedService$` subscription.
    this.serviceState.deleteMediaFromService(this.serviceId, mediaId).subscribe({
      error: (err) => console.error('Delete media failed', err)
    });
  }

  // Handles service deletion with confirmation.
  deleteService(): void {
    if (!this.serviceId) {
      this.toastr.error('Service ID is missing. Cannot delete.');
      return;
    }
    // For a better UX, you could replace this with a custom confirmation modal.
    const isConfirmed = window.confirm(
      'Are you sure you want to delete this service? This action cannot be undone.'
    );
    if (isConfirmed) {
      this.serviceState
        .deleteService(this.serviceId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          // On successful deletion, navigate away from this page.
          complete: () => this.router.navigate(['/services']),
        });
    }
  }

  // Handles form submission for updates.
  onSubmit(): void {
    this.serviceForm.markAllAsTouched();
    if (this.serviceForm.invalid) {
      this.toastr.error('Please correct the errors in the form before submitting.');
      return;
    }
    // To check if User made some changes
    if (!this.serviceForm.dirty) {
      this.toastr.info('No changes were made to the service fields.');
      this.router.navigate(['/services']);
      return;
    }
    this.isSubmitting = true;
    // We only need to submit the non-media fields, as media is handled separately and immediately.
    const { media, ...serviceData } = this.serviceForm.value;
    this.serviceState.updateService(this.serviceId, serviceData).pipe(
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: () => {
        this.router.navigate(['/services']);
      },
      error: (err) => {
        // Error is handled by the state service.
        console.error('Failed to update service', err);
      }
    });
  }
}