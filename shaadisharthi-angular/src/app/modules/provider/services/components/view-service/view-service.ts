// Component for viewing service details, including media tabs.

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Media, Service } from '../../models/service.model';
import { ServiceStateService } from '../../services/service-state.service';

// Defining the component as standalone.
@Component({
  selector: 'app-view-service',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './view-service.html',
  styleUrls: ['./view-service.scss']
})
export class ViewServiceComponent implements OnInit {
  // Injecting route and state service.
  private readonly route = inject(ActivatedRoute);
  private readonly serviceState = inject(ServiceStateService);

  // Observables for service and loading.
  public readonly service$: Observable<Service | null>;
  public readonly isLoading$: Observable<boolean>;

  constructor() {
    this.service$ = this.serviceState.selectedService$;
    this.isLoading$ = this.serviceState.isLoading$;
  }

  // Lifecycle hook to load service based on route ID.
  ngOnInit(): void {
    // Using the observable paramMap is more robust than the snapshot.
    this.route.paramMap.pipe(tap(params => {
      const id = params.get('id');
      if (id) this.serviceState.selectService(parseInt(id, 10));
    })).subscribe();
  }

  // Helper method to filter media by type.
  public getMediaByType(media: Media[] | undefined | null, type: 'image' | 'video'): Media[] {
    if (!media) {
      return [];
    }
    return media.filter(m => m.type.toLowerCase() === type);
  }
}